package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.VisitStatistics;
import fi.newdoska.doska.repository.VisitStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class VisitStatsService {

    private final VisitStatisticsRepository repository;

    public record VisitStatsDto(long totalVisits, long todayVisits) {}

    @Transactional(readOnly = true)
    public VisitStatsDto getStats() {
        return repository.findById(1)
                .map(v -> new VisitStatsDto(
                        v.getTotalVisits() != null ? v.getTotalVisits() : 0L,
                        v.getTodayVisits() != null ? v.getTodayVisits() : 0L))
                .orElse(new VisitStatsDto(0, 0));
    }

    @Transactional
    public void recordSessionVisit() {
        VisitStatistics stats = repository.findForUpdate().orElseGet(this::createDefault);
        LocalDate today = LocalDate.now();
        if (!today.equals(stats.getStatDate())) {
            stats.setStatDate(today);
            stats.setTodayVisits(0L);
        }
        stats.setTotalVisits(stats.getTotalVisits() + 1);
        stats.setTodayVisits(stats.getTodayVisits() + 1);
        repository.save(stats);
    }

    private VisitStatistics createDefault() {
        return repository.save(VisitStatistics.builder()
                .id(1)
                .totalVisits(0L)
                .todayVisits(0L)
                .statDate(LocalDate.now())
                .build());
    }
}
