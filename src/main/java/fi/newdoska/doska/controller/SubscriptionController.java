package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.Category;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.CategoryRepository;
import fi.newdoska.doska.service.CategorySubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    
    private final CategorySubscriptionService subscriptionService;
    private final CategoryRepository categoryRepository;
    
    @GetMapping
    public String showSubscriptions(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("subscriptions", subscriptionService.getUserSubscriptions(user));
        // Получаем все категории, если активных нет - используем все
        List<Category> activeCategories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
        if (activeCategories.isEmpty()) {
            activeCategories = categoryRepository.findAll();
        }
        model.addAttribute("categories", activeCategories);
        return "subscriptions";
    }
    
    @PostMapping("/subscribe")
    @ResponseBody
    public String subscribe(@AuthenticationPrincipal User user, @RequestParam Long categoryId) {
        try {
            subscriptionService.subscribe(user, categoryId);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    
    @PostMapping("/unsubscribe")
    @ResponseBody
    public String unsubscribe(@AuthenticationPrincipal User user, @RequestParam Long categoryId) {
        try {
            subscriptionService.unsubscribe(user, categoryId);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
}

