package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.Category;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.CategoryRepository;
import fi.newdoska.doska.service.CategorySubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionApiController {
    
    private final CategorySubscriptionService subscriptionService;
    private final CategoryRepository categoryRepository;
    
    @PostMapping("/subscribe/{categoryId}")
    public ResponseEntity<Map<String, Object>> subscribe(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (user == null) {
            response.put("success", false);
            response.put("error", "Требуется авторизация");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
            
            subscriptionService.subscribe(user, categoryId);
            response.put("success", true);
            response.put("message", "Вы успешно подписались на категорию");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/unsubscribe/{categoryId}")
    public ResponseEntity<Map<String, Object>> unsubscribe(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (user == null) {
            response.put("success", false);
            response.put("error", "Требуется авторизация");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            subscriptionService.unsubscribe(user, categoryId);
            response.put("success", true);
            response.put("message", "Вы отписались от категории");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}
