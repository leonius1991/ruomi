package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.service.AdvertisementService;
import fi.newdoska.doska.service.ModerationLogService;
import fi.newdoska.doska.service.SearchAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/moderator")
@RequiredArgsConstructor
public class ModeratorController {

    private final AdvertisementService advertisementService;
    private final SearchAnalyticsService searchAnalyticsService;
    private final ModerationLogService moderationLogService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Page<Advertisement> pending = advertisementService.getPendingAdvertisements(0, 10);
        model.addAttribute("pendingAdvertisements", pending.getContent());
        model.addAttribute("pendingCount", pending.getTotalElements());
        model.addAttribute("recentSearches", searchAnalyticsService.getRecentSearches(10));
        model.addAttribute("topQueries", searchAnalyticsService.getTopQueries(10));
        model.addAttribute("zeroResultQueries", searchAnalyticsService.getZeroResultSearches(10));
        model.addAttribute("moderationLogs", moderationLogService.getAllLogs(0, 20).getContent());
        model.addAttribute("searchSummary", searchAnalyticsService.getSummary());
        return "moderator/dashboard";
    }

    @PostMapping("/advertisement/{id}/approve")
    public String approveAdvertisement(
            @PathVariable Long id,
            @RequestParam(required = false) String comment,
            RedirectAttributes redirectAttributes) {
        try {
            String username = getCurrentUsername();
            advertisementService.approveAdvertisement(id, username, comment != null ? comment : "Одобрено модератором");
            redirectAttributes.addFlashAttribute("success", "Объявление одобрено");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/moderator/dashboard";
    }

    @PostMapping("/advertisement/{id}/reject")
    public String rejectAdvertisement(
            @PathVariable Long id,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {
        try {
            String username = getCurrentUsername();
            advertisementService.rejectAdvertisement(id, reason, username);
            redirectAttributes.addFlashAttribute("success", "Объявление отклонено");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/moderator/dashboard";
    }

    @GetMapping("/advertisement/{id}/logs")
    public String getAdvertisementLogs(@PathVariable Long id, Model model) {
        model.addAttribute("logs", moderationLogService.getLogsByAdvertisement(id, 0, 50).getContent());
        model.addAttribute("advertisement", advertisementService.getAdvertisementById(id).orElse(null));
        return "moderator/advertisement-logs";
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getUsername();
        }
        return authentication.getName();
    }
}
