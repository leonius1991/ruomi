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

    public static final String DEFAULT_LANE = "A/B";
    public static final List<String> LANES = List.of("A/B", "BC", "C", "CE", "D");

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
        BorderLaneStats laneStats = buildLaneStats(latestLanes);

        int koidulaLive = laneStats.liveFor(DEFAULT_LANE, "KOIDULA");
        int luhamaaLive = laneStats.liveFor(DEFAULT_LANE, "LUHAMAA");
        long koidulaToday = laneStats.todayFor(DEFAULT_LANE, "KOIDULA");
        long luhamaaToday = laneStats.todayFor(DEFAULT_LANE, "LUHAMAA");

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
                laneStats,
                eventRepository.findTop50ByOrderByRecordedAtDesc(),
                lastUpdate
        );
    }

    private BorderLaneStats buildLaneStats(List<BorderQueueSnapshot> latestLanes) {
        Map<String, Map<String, Integer>> live = new LinkedHashMap<>();
        for (BorderQueueSnapshot snapshot : latestLanes) {
            live.computeIfAbsent(snapshot.getLane(), k -> new LinkedHashMap<>())
                    .put(snapshot.getCheckpoint(), snapshot.getLiveCount());
        }

        LocalDateTime from = LocalDate.now().minusDays(13).atStartOfDay();
        Map<String, Map<String, Map<String, Long>>> daily = new LinkedHashMap<>();
        for (Object[] row : eventRepository.sumDailyExitsByLaneSince(from)) {
            String checkpoint = String.valueOf(row[0]);
            String lane = String.valueOf(row[1]);
            String day = String.valueOf(row[2]);
            long total = ((Number) row[3]).longValue();
            daily.computeIfAbsent(lane, k -> new LinkedHashMap<>())
                    .computeIfAbsent(checkpoint, k -> new TreeMap<>())
                    .put(day, total);
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Map<String, Map<String, Long>> today = new LinkedHashMap<>();
        for (BorderQueueEvent event : eventRepository.findByRecordedAtAfterOrderByRecordedAtAsc(startOfDay)) {
            if (event.getEventType() != BorderQueueEvent.EventType.EXIT) {
                continue;
            }
            today.computeIfAbsent(event.getLane(), k -> new LinkedHashMap<>())
                    .merge(event.getCheckpoint(), (long) event.getDelta(), Long::sum);
        }

        return new BorderLaneStats(live, daily, today);
    }

    public record BorderLaneStats(
            Map<String, Map<String, Integer>> liveByLane,
            Map<String, Map<String, Map<String, Long>>> dailyExitsByLane,
            Map<String, Map<String, Long>> todayExitsByLane
    ) {
        public int liveFor(String lane, String checkpoint) {
            if ("ALL".equals(lane)) {
                return liveByLane.values().stream()
                        .mapToInt(m -> m.getOrDefault(checkpoint, 0))
                        .sum();
            }
            return liveByLane.getOrDefault(lane, Map.of()).getOrDefault(checkpoint, 0);
        }

        public long todayFor(String lane, String checkpoint) {
            if ("ALL".equals(lane)) {
                return todayExitsByLane.values().stream()
                        .mapToLong(m -> m.getOrDefault(checkpoint, 0L))
                        .sum();
            }
            return todayExitsByLane.getOrDefault(lane, Map.of()).getOrDefault(checkpoint, 0L);
        }

        public Map<String, Long> dailyForCheckpoint(String lane, String checkpoint) {
            if ("ALL".equals(lane)) {
                Map<String, Long> merged = new TreeMap<>();
                for (Map<String, Map<String, Long>> byCheckpoint : dailyExitsByLane.values()) {
                    Map<String, Long> days = byCheckpoint.get(checkpoint);
                    if (days == null) {
                        continue;
                    }
                    days.forEach((day, total) -> merged.merge(day, total, Long::sum));
                }
                return merged;
            }
            return dailyExitsByLane.getOrDefault(lane, Map.of()).getOrDefault(checkpoint, Map.of());
        }
    }

    public record BorderQueueDashboard(
            int koidulaLive,
            int luhamaaLive,
            long koidulaToday,
            long luhamaaToday,
            List<BorderQueueSnapshot> latestLanes,
            BorderLaneStats laneStats,
            List<BorderQueueEvent> recentEvents,
            LocalDateTime lastUpdate
    ) {}
}
