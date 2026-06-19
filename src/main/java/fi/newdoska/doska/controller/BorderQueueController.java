package fi.newdoska.doska.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.newdoska.doska.service.BorderQueueAnalyticsService;
import fi.newdoska.doska.service.BorderQueueTrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class BorderQueueController {

    private final BorderQueueTrackerService trackerService;
    private final BorderQueueAnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    @GetMapping("/border-queues")
    public String page(Model model) throws JsonProcessingException {
        BorderQueueTrackerService.BorderQueueDashboard dashboard = trackerService.getDashboard();
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("laneStatsJson", objectMapper.writeValueAsString(dashboard.laneStats()));
        model.addAttribute("defaultLane", BorderQueueTrackerService.DEFAULT_LANE);
        model.addAttribute("pageTitle", "Очередь на границе — Koidula и Luhamaa | ruomi.fi");
        model.addAttribute("metaDescription", "Live queue и статистика проезда через границу Estonia–Россия. Koidula, Luhamaa.");
        model.addAttribute("metaKeywords", "очередь на границе, koidula, luhamaa, estonia border");
        model.addAttribute("analytics", analyticsService.buildAnalytics(BorderQueueTrackerService.DEFAULT_LANE));
        return "border-queues";
    }

    @GetMapping("/api/border-queues/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> statsApi() throws JsonProcessingException {
        BorderQueueTrackerService.BorderQueueDashboard d = trackerService.getDashboard();
        Map<String, Object> body = new HashMap<>();
        body.put("koidulaLive", d.koidulaLive());
        body.put("luhamaaLive", d.luhamaaLive());
        body.put("koidulaToday", d.koidulaToday());
        body.put("luhamaaToday", d.luhamaaToday());
        body.put("laneStats", d.laneStats());
        body.put("lastUpdate", d.lastUpdate());
        return ResponseEntity.ok(body);
    }
}
