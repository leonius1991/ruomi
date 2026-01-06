package fi.newdoska.doska.service;

import fi.newdoska.doska.dto.SearchStat;
import fi.newdoska.doska.entity.SearchLog;
import fi.newdoska.doska.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchAnalyticsService {

    private final SearchLogRepository searchLogRepository;
    private final EmailService emailService;

    @Value("${analytics.alert.email:}")
    private String alertEmail;

    @Transactional
    public void logSearch(String term,
                          String category,
                          String city,
                          long resultsCount,
                          String username) {
        if (term == null || term.isBlank()) {
            return;
        }
        SearchLog logEntry = new SearchLog();
        logEntry.setTerm(term.trim());
        logEntry.setCategory(category != null ? category : "");
        logEntry.setCity(city != null ? city : "");
        logEntry.setResultsCount(resultsCount);
        logEntry.setUsername(username);
        logEntry.setCreatedAt(LocalDateTime.now());
        searchLogRepository.save(logEntry);

        if (resultsCount == 0 && alertEmail != null && !alertEmail.isBlank()) {
            try {
                emailService.sendSearchAlert(alertEmail, term, category, city);
            } catch (Exception e) {
                log.warn("Failed to send search alert for query {}", term, e);
            }
        }
    }

    public List<SearchLog> getRecentSearches(int limit) {
        return searchLogRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<SearchLog> getZeroResultSearches(int limit) {
        return searchLogRepository.findTop10ByResultsCountOrderByCreatedAtDesc(0L)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<SearchStat> getTopQueries(int limit) {
        return searchLogRepository.findTopTerms(PageRequest.of(0, limit)).stream()
                .map(row -> new SearchStat((String) row[0], ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }
}

