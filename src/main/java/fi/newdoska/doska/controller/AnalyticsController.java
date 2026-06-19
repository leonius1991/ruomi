package fi.newdoska.doska.controller;

import fi.newdoska.doska.service.SearchAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/moderator/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
public class AnalyticsController {

    private final SearchAnalyticsService searchAnalyticsService;

    @GetMapping
    public String analytics(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String city,
            Model model) {
        model.addAttribute("topQueries", searchAnalyticsService.getTopQueries(20));
        model.addAttribute("recentSearches", searchAnalyticsService.getRecentSearches(50));
        model.addAttribute("zeroResultSearches", searchAnalyticsService.getZeroResultSearches(20));
        model.addAttribute("topCities", searchAnalyticsService.getTopCities(10));
        model.addAttribute("summary", searchAnalyticsService.getSummary());
        return "moderator/analytics";
    }

    @GetMapping("/export/csv")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String city) {
        
        List<String> lines = searchAnalyticsService.getRecentSearches(1000).stream()
                .map(log -> String.format("%s,%s,%s,%s,%s,%s",
                        escapeCsv(log.getTerm()),
                        escapeCsv(log.getCategory() != null ? log.getCategory() : ""),
                        escapeCsv(log.getCity() != null ? log.getCity() : ""),
                        log.getResultsCount(),
                        escapeCsv(log.getUsername() != null ? log.getUsername() : "anonymous"),
                        log.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .collect(Collectors.toList());

        String csv = "Запрос,Категория,Город,Найдено,Пользователь,Время\n" + String.join("\n", lines);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", 
                "analytics_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}


