package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.service.TelegramAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TelegramAuthController {
    
    private final TelegramAuthService telegramAuthService;
    private final fi.newdoska.doska.service.UserService userService;
    
    /**
     * Handle Telegram login callback
     */
    @PostMapping("/auth/telegram/callback")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleTelegramCallback(@RequestBody Map<String, String> telegramData,
                                                                      HttpServletRequest request) {
        try {
            log.info("Received Telegram callback data: {}", telegramData);
            
            Map<String, Object> result = telegramAuthService.verifyTelegramLogin(telegramData);
            
            if ((Boolean) result.get("success")) {
                User user = (User) result.get("user");
                authenticateUser(user);
                request.getSession(true).setAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        SecurityContextHolder.getContext()
                );
                result.put("user", Map.of("username", user.getUsername(), "role", user.getRole()));
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            log.error("Error processing Telegram callback", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Internal server error"
            ));
        }
    }
    
    /**
     * Link Telegram account to existing user
     */
    @PostMapping("/profile/link-telegram")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> linkTelegramAccount(
            @RequestParam(required = false) Long telegramId,
            @RequestParam(required = false) String telegramUsername,
            @RequestParam(required = false) String photoUrl) {
        
        try {
            User currentUser = getAuthenticatedUser();
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "Требуется авторизация"
                ));
            }
            
            // Если telegramId не передан, значит это вызов из виджета
            if (telegramId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Данные Telegram не получены"
                ));
            }
            
            boolean success;
            if (photoUrl != null && !photoUrl.isEmpty()) {
                success = telegramAuthService.linkTelegramToUserWithPhoto(
                    currentUser.getId(), telegramId, telegramUsername, photoUrl);
            } else {
                success = telegramAuthService.linkTelegramToUser(
                    currentUser.getId(), telegramId, telegramUsername);
            }
            
            if (success) {
                // Обновляем пользователя из БД
                currentUser = userService.findById(currentUser.getId()).orElse(currentUser);
                currentUser.setTelegramId(telegramId);
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    currentUser.setAvatarUrl(photoUrl);
                }
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Telegram account linked successfully",
                    "username", telegramUsername != null ? telegramUsername : ""
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Не удалось связать аккаунт. Возможно, этот Telegram уже привязан к другому аккаунту."
                ));
            }
            
        } catch (Exception e) {
            log.error("Error linking Telegram account", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Internal server error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Unlink Telegram account from user
     */
    @PostMapping("/profile/unlink-telegram")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unlinkTelegramAccount() {
        
        try {
            User currentUser = getAuthenticatedUser();
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "Требуется авторизация"
                ));
            }
            
            boolean success = telegramAuthService.unlinkTelegramFromUser(currentUser.getId());
            
            if (success) {
                currentUser.setTelegramId(null);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Telegram account unlinked successfully"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to unlink Telegram account"
                ));
            }
            
        } catch (Exception e) {
            log.error("Error unlinking Telegram account", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Internal server error"
            ));
        }
    }
    
    /**
     * Show Telegram linking page
     */
    @GetMapping("/profile/link-telegram")
    public String showTelegramLinkingPage(Model model) {
        return "profile/link-telegram";
    }

    private void authenticateUser(User user) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
    
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        return null;
    }
}



