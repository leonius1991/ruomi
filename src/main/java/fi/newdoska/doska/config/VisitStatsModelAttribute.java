package fi.newdoska.doska.config;

import fi.newdoska.doska.service.VisitStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class VisitStatsModelAttribute {

    private final VisitStatsService visitStatsService;

    @ModelAttribute("visitStats")
    public VisitStatsService.VisitStatsDto visitStats() {
        return visitStatsService.getStats();
    }
}
