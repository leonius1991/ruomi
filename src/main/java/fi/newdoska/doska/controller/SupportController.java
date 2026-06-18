package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.service.SupportService;
import fi.newdoska.doska.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/support")
@RequiredArgsConstructor
@Slf4j
public class SupportController {
    
    private final SupportService supportService;
    
    @GetMapping
    public String supportPage(Model model) {
        return "support";
    }
    
    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendSupportMessage(
            @AuthenticationPrincipal User user,
            @RequestParam String subject,
            @RequestParam String message,
            @RequestParam(required = false) String email) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String userEmail = email != null && !email.trim().isEmpty() 
                ? email.trim() 
                : (user != null ? user.getEmail() : null);
            
            if (userEmail == null || userEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Email адрес обязателен");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (message == null || message.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Сообщение не может быть пустым");
                return ResponseEntity.badRequest().body(response);
            }
            
            supportService.sendSupportMessage(userEmail, subject, message, user);
            
            response.put("success", true);
            response.put("message", "Ваше сообщение успешно отправлено в поддержку. Мы ответим вам в ближайшее время.");
            log.info("Support message sent from: {} (user: {})", userEmail, user != null ? user.getUsername() : "anonymous");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error sending support message", e);
            response.put("success", false);
            response.put("error", "Ошибка при отправке сообщения: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

