package fi.newdoska.doska.service;

import fi.newdoska.doska.repository.BorderQueueEventRepository;
import fi.newdoska.doska.repository.BorderQueueSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BorderQueueAnalyticsService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String[] DAY_NAMES = {"", "Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};

    private final BorderQueueSnapshotRepository snapshotRepository;
    private final BorderQueueEventRepository eventRepository;

    public BorderAnalytics buildAnalytics(String defaultLane) {
        String lane = defaultLane != null ? defaultLane : BorderQueueTrackerService.DEFAULT_LANE;

        List<HourlyQueueStat> hourlyQueue = buildHourlyQueueStats();
        List<PeakQueueStat> peakQueues = buildPeakQueueStats();
        List<HourlyExitStat> hourlyExits = buildHourlyExitStats(lane);
        List<DayOfWeekExitStat> weeklyExits = buildWeeklyExitStats(lane);

        HourlyQueueStat bestKoidula = bestHourForCheckpoint(hourlyQueue, "KOIDULA", lane);
        HourlyQueueStat bestLuhamaa = bestHourForCheckpoint(hourlyQueue, "LUHAMAA", lane);
        HourlyQueueStat bestKoidulaAny = bestHourForCheckpoint(hourlyQueue, "KOIDULA", null);
        HourlyQueueStat bestLuhamaaAny = bestHourForCheckpoint(hourlyQueue, "LUHAMAA", null);

        Map<String, Long> totalExits = new LinkedHashMap<>();
        for (Object[] row : eventRepository.sumTotalExitsByCheckpoint()) {
            totalExits.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        Map<String, Double> avgQueue = new LinkedHashMap<>();
        for (Object[] row : snapshotRepository.findOverallAvgLiveCount()) {
            String key = cpLabel(String.valueOf(row[0])) + " · " + row[1];
            avgQueue.put(key, round(((Number) row[2]).doubleValue(), 1));
        }

        LocalDateTime sinceSnapshot = snapshotRepository.findEarliestSnapshotTime();
        LocalDateTime sinceEvent = eventRepository.findEarliestEventTime();
        LocalDateTime since = minNonNull(sinceSnapshot, sinceEvent);

        long snapshots = snapshotRepository.countSnapshots();
        long exitEvents = eventRepository.countExitEvents();

        List<HourlyQueueStat> topLowQueueHours = hourlyQueue.stream()
                .filter(h -> lane.equals(h.lane()))
                .sorted(Comparator.comparingDouble(HourlyQueueStat::avgQueue))
                .limit(5)
                .toList();

        List<HourlyExitStat> topExitHours = hourlyExits.stream().limit(5).toList();
        List<DayOfWeekExitStat> topExitDays = weeklyExits.stream().limit(7).toList();

        return new BorderAnalytics(
                lane,
                bestKoidula,
                bestLuhamaa,
                bestKoidulaAny,
                bestLuhamaaAny,
                topLowQueueHours,
                peakQueues,
                topExitHours,
                topExitDays,
                totalExits.getOrDefault("KOIDULA", 0L),
                totalExits.getOrDefault("LUHAMAA", 0L),
                avgQueue,
                snapshots,
                exitEvents,
                since != null ? since.format(FMT) : "—"
        );
    }

    private List<HourlyQueueStat> buildHourlyQueueStats() {
        List<HourlyQueueStat> list = new ArrayList<>();
        for (Object[] row : snapshotRepository.findAvgLiveCountByHour()) {
            list.add(new HourlyQueueStat(
                    String.valueOf(row[0]),
                    String.valueOf(row[1]),
                    ((Number) row[2]).intValue(),
                    round(((Number) row[3]).doubleValue(), 1),
                    formatHourRange(((Number) row[2]).intValue())
            ));
        }
        return list;
    }

    private List<PeakQueueStat> buildPeakQueueStats() {
        List<PeakQueueStat> list = new ArrayList<>();
        for (Object[] row : snapshotRepository.findMaxLiveCountByCheckpointAndLane()) {
            list.add(new PeakQueueStat(
                    cpLabel(String.valueOf(row[0])),
                    String.valueOf(row[1]),
                    ((Number) row[2]).intValue()
            ));
        }
        list.sort(Comparator.comparingInt(PeakQueueStat::maxQueue).reversed());
        return list;
    }

    private List<HourlyExitStat> buildHourlyExitStats(String lane) {
        List<HourlyExitStat> list = new ArrayList<>();
        for (Object[] row : eventRepository.sumExitsByHourAndCheckpoint(lane)) {
            list.add(new HourlyExitStat(
                    cpLabel(String.valueOf(row[1])),
                    ((Number) row[0]).intValue(),
                    formatHourRange(((Number) row[0]).intValue()),
                    ((Number) row[2]).longValue()
            ));
        }
        list.sort(Comparator.comparingLong(HourlyExitStat::totalExits).reversed());
        return list;
    }

    private List<DayOfWeekExitStat> buildWeeklyExitStats(String lane) {
        List<DayOfWeekExitStat> list = new ArrayList<>();
        for (Object[] row : eventRepository.sumExitsByDayOfWeekAndCheckpoint(lane)) {
            int dow = ((Number) row[0]).intValue();
            list.add(new DayOfWeekExitStat(
                    cpLabel(String.valueOf(row[1])),
                    dayName(dow),
                    dow,
                    ((Number) row[2]).longValue()
            ));
        }
        list.sort(Comparator.comparingLong(DayOfWeekExitStat::totalExits).reversed());
        return list;
    }

    private HourlyQueueStat bestHourForCheckpoint(List<HourlyQueueStat> stats, String checkpoint, String lane) {
        return stats.stream()
                .filter(s -> checkpoint.equals(s.checkpoint()))
                .filter(s -> lane == null || lane.equals(s.lane()))
                .min(Comparator.comparingDouble(HourlyQueueStat::avgQueue))
                .orElse(null);
    }

    private static String cpLabel(String cp) {
        return "KOIDULA".equals(cp) ? "Koidula" : "Luhamaa";
    }

    private static String formatHourRange(int hour) {
        return String.format("%02d:00–%02d:59", hour, hour);
    }

    private static String dayName(int dow) {
        return dow >= 1 && dow <= 7 ? DAY_NAMES[dow] : "—";
    }

    private static double round(double v, int places) {
        double m = Math.pow(10, places);
        return Math.round(v * m) / m;
    }

    private static LocalDateTime minNonNull(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    public record HourlyQueueStat(String checkpoint, String lane, int hour, double avgQueue, String hourLabel) {}

    public record PeakQueueStat(String checkpoint, String lane, int maxQueue) {}

    public record HourlyExitStat(String checkpoint, int hour, String hourLabel, long totalExits) {}

    public record DayOfWeekExitStat(String checkpoint, String dayName, int dayOfWeek, long totalExits) {}

    public record BorderAnalytics(
            String defaultLane,
            HourlyQueueStat bestBookingKoidula,
            HourlyQueueStat bestBookingLuhamaa,
            HourlyQueueStat bestBookingKoidulaAllLanes,
            HourlyQueueStat bestBookingLuhamaaAllLanes,
            List<HourlyQueueStat> topLowestQueueHours,
            List<PeakQueueStat> peakQueues,
            List<HourlyExitStat> topExitHours,
            List<DayOfWeekExitStat> topExitDays,
            long totalExitsKoidula,
            long totalExitsLuhamaa,
            Map<String, Double> avgQueueByLane,
            long snapshotCount,
            long exitEventCount,
            String dataSince
    ) {}
}
