package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.BorderQueueEvent;
import fi.newdoska.doska.entity.BorderQueueSnapshot;
import fi.newdoska.doska.repository.BorderQueueEventRepository;
import fi.newdoska.doska.repository.BorderQueueSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BorderQueueTrackerService {

    private final EstonianBorderQueueParser parser;
    private final BorderQueueSnapshotRepository snapshotRepository;
    private final BorderQueueEventRepository eventRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void pollAndProcess() {
        String html = restTemplate.getForObject(parser.getSourceUrl(), String.class);
        if (html == null || html.isBlank()) {
            log.warn("Пустой ответ от estonianborder.eu");
            return;
        }

        EstonianBorderQueueParser.ParsedQueueData data = parser.parse(html);
        LocalDateTime now = LocalDateTime.now();

        for (EstonianBorderQueueParser.LaneReading lane : EstonianBorderQueueParser.LANES) {
            String key = lane.checkpoint() + ":" + lane.lane();
            Integer newCount = data.liveByLane().get(key);
            if (newCount == null) {
                continue;
            }

            Optional<BorderQueueSnapshot> previousOpt = snapshotRepository
                    .findFirstByCheckpointAndLaneOrderByCapturedAtDesc(lane.checkpoint(), lane.lane());

            snapshotRepository.save(BorderQueueSnapshot.builder()
                    .checkpoint(lane.checkpoint())
                    .lane(lane.lane())
                    .liveCount(newCount)
                    .capturedAt(now)
                    .build());

            if (previousOpt.isEmpty()) {
                continue;
            }

            int previousCount = previousOpt.get().getLiveCount();
            if (Objects.equals(previousCount, newCount)) {
                continue;
            }

            if (newCount < previousCount) {
                int delta = previousCount - newCount;
                eventRepository.save(BorderQueueEvent.builder()
                        .checkpoint(lane.checkpoint())
                        .lane(lane.lane())
                        .eventType(BorderQueueEvent.EventType.EXIT)
                        .delta(delta)
                        .previousCount(previousCount)
                        .newCount(newCount)
                        .recordedAt(now)
                        .build());
                log.info("Граница {} {}: проехало ~{} машин ({} -> {})",
                        lane.checkpoint(), lane.lane(), delta, previousCount, newCount);
            } else {
                int delta = newCount - previousCount;
                eventRepository.save(BorderQueueEvent.builder()
                        .checkpoint(lane.checkpoint())
                        .lane(lane.lane())
                        .eventType(BorderQueueEvent.EventType.ENTER)
                        .delta(delta)
                        .previousCount(previousCount)
                        .newCount(newCount)
                        .recordedAt(now)
                        .build());
            }
        }
    }

    public BorderQueueDashboard getDashboard() {
        List<BorderQueueSnapshot> latestLanes = snapshotRepository.findLatestPerLane();
        int koidulaLive = sumCheckpoint(latestLanes, "KOIDULA");
        int luhamaaLive = sumCheckpoint(latestLanes, "LUHAMAA");

        LocalDateTime from = LocalDate.now().minusDays(13).atStartOfDay();
        List<Object[]> dailyRows = eventRepository.sumDailyExitsSince(from);

        Map<String, Map<String, Long>> dailyByCheckpoint = new TreeMap<>();
        for (Object[] row : dailyRows) {
            String checkpoint = String.valueOf(row[0]);
            String day = String.valueOf(row[1]);
            long total = ((Number) row[2]).longValue();
            dailyByCheckpoint.computeIfAbsent(checkpoint, k -> new TreeMap<>()).put(day, total);
        }

        long koidulaToday = exitsToday("KOIDULA");
        long luhamaaToday = exitsToday("LUHAMAA");

        LocalDateTime lastUpdate = latestLanes.stream()
                .map(BorderQueueSnapshot::getCapturedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new BorderQueueDashboard(
                koidulaLive,
                luhamaaLive,
                koidulaToday,
                luhamaaToday,
                latestLanes,
                dailyByCheckpoint,
                eventRepository.findTop50ByOrderByRecordedAtDesc(),
                lastUpdate
        );
    }

    private long exitsToday(String checkpoint) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return eventRepository.findByRecordedAtAfterOrderByRecordedAtAsc(start).stream()
                .filter(e -> e.getCheckpoint().equals(checkpoint))
                .filter(e -> e.getEventType() == BorderQueueEvent.EventType.EXIT)
                .mapToLong(BorderQueueEvent::getDelta)
                .sum();
    }

    private int sumCheckpoint(List<BorderQueueSnapshot> snapshots, String checkpoint) {
        return snapshots.stream()
                .filter(s -> checkpoint.equals(s.getCheckpoint()))
                .mapToInt(BorderQueueSnapshot::getLiveCount)
                .sum();
    }

    public record BorderQueueDashboard(
            int koidulaLive,
            int luhamaaLive,
            long koidulaToday,
            long luhamaaToday,
            List<BorderQueueSnapshot> latestLanes,
            Map<String, Map<String, Long>> dailyExits,
            List<BorderQueueEvent> recentEvents,
            LocalDateTime lastUpdate
    ) {}
}
