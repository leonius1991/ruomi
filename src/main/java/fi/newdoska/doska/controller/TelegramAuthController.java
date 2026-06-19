package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.service.TelegramAuthService;
import fi.newdoska.doska.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TelegramAuthController {

    private final TelegramAuthService telegramAuthService;
    private final UserService userService;

    @PostMapping("/auth/telegram/callback")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleTelegramCallback(@RequestBody Map<String, Object> rawData,
                                                                      HttpServletRequest request) {
        try {
            log.info("Received Telegram callback for id={}", rawData.get("id"));

            Map<String, Object> result = telegramAuthService.verifyTelegramLogin(rawData);

            if (Boolean.TRUE.equals(result.get("success"))) {
                User user = (User) result.get("user");
                User freshUser = userService.findById(user.getId()).orElse(user);
                persistAuthentication(freshUser, request);
                result.put("user", Map.of(
                        "username", freshUser.getUsername(),
                        "role", freshUser.getRole().name()
                ));
                if (result.containsKey("temporaryPassword")) {
                    result.put("redirectUrl", "/");
                }
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.badRequest().body(result);

        } catch (Exception e) {
            log.error("Error processing Telegram callback", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Внутренняя ошибка сервера"
            ));
        }
    }

    @PostMapping("/profile/link-telegram")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> linkTelegramAccount(@RequestBody Map<String, Object> rawData) {
        try {
            User currentUser = getAuthenticatedUser();
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "error", "Требуется авторизация"
                ));
            }

            Map<String, Object> result = telegramAuthService.linkVerifiedTelegramToUser(
                    currentUser.getId(), telegramAuthService.normalizeTelegramData(rawData));

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.badRequest().body(result);

        } catch (Exception e) {
            log.error("Error linking Telegram account", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Ошибка привязки: " + e.getMessage()
            ));
        }
    }

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
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Telegram отвязан"
                ));
            }
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Не удалось отвязать Telegram"
            ));

        } catch (Exception e) {
            log.error("Error unlinking Telegram account", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Внутренняя ошибка сервера"
            ));
        }
    }

    @GetMapping("/profile/link-telegram")
    public String redirectToProfileTelegram() {
        return "redirect:/profile#telegram";
    }

    private void persistAuthentication(User user, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticationToken);
        SecurityContextHolder.setContext(context);

        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return userService.findById(user.getId()).orElse(user);
        }
        try {
            return (User) userService.loadUserByUsername(auth.getName());
        } catch (Exception e) {
            return null;
        }
    }
}
