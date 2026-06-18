package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.UserRepository;
import fi.newdoska.doska.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-menu")
@RequiredArgsConstructor
@Slf4j
public class UserMenuController {
    
    private final UserService userService;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    
    @GetMapping("/info/{userId}")
    public Map<String, Object> getUserInfo(@PathVariable Long userId, 
                                           @AuthenticationPrincipal User currentUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
            
            response.put("success", true);
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                "lastName", user.getLastName() != null ? user.getLastName() : "",
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "/images/default-avatar.png"
            ));
            response.put("canMessage", !user.getId().equals(currentUser.getId()));
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
    
    @PostMapping("/report")
    public Map<String, Object> reportUser(@RequestParam Long userId,
                                          @RequestParam String reason,
                                          @RequestParam(required = false) String description,
                                          @AuthenticationPrincipal User currentUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            User reportedUser = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
            
            if (currentUser == null) {
                response.put("success", false);
                response.put("error", "Необходима авторизация");
                return response;
            }
            
            if (reportedUser.getId().equals(currentUser.getId())) {
                response.put("success", false);
                response.put("error", "Вы не можете пожаловаться на себя");
                return response;
            }
            
            // Получаем всех админов, супер админов и модераторов
            List<User> admins = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == User.UserRole.SUPER_ADMIN || 
                                   user.getRole() == User.UserRole.ADMIN || 
                                   user.getRole() == User.UserRole.MODERATOR)
                    .collect(Collectors.toList());
            
            // Формируем сообщение
            String reportContent = String.format(
                "Жалоба на пользователя\n\n" +
                "Жалобу подал: %s %s (@%s, ID: %d)\n" +
                "Email: %s\n\n" +
                "На пользователя: %s %s (@%s, ID: %d)\n" +
                "Email: %s\n\n" +
                "Причина: %s\n" +
                "Описание: %s\n\n" +
                "---\n" +
                "Это автоматическое уведомление от ruomi.fi",
                currentUser.getFirstName() != null ? currentUser.getFirstName() : "",
                currentUser.getLastName() != null ? currentUser.getLastName() : "",
                currentUser.getUsername(),
                currentUser.getId(),
                currentUser.getEmail() != null ? currentUser.getEmail() : "Не указан",
                reportedUser.getFirstName() != null ? reportedUser.getFirstName() : "",
                reportedUser.getLastName() != null ? reportedUser.getLastName() : "",
                reportedUser.getUsername(),
                reportedUser.getId(),
                reportedUser.getEmail() != null ? reportedUser.getEmail() : "Не указан",
                reason,
                description != null ? description : "Не указано"
            );
            
            // Отправляем письмо каждому админу/модератору
            int sentCount = 0;
            for (User admin : admins) {
                if (admin.getEmail() != null && !admin.getEmail().trim().isEmpty()) {
                    try {
                        SimpleMailMessage mailMessage = new SimpleMailMessage();
                        mailMessage.setTo(admin.getEmail());
                        mailMessage.setSubject("[ruomi.fi Жалоба] Жалоба на пользователя @" + reportedUser.getUsername());
                        mailMessage.setText(reportContent);
                        mailMessage.setFrom("noreply@ruomi.fi");
                        
                        mailSender.send(mailMessage);
                        sentCount++;
                        log.info("Report sent to admin: {}", admin.getEmail());
                    } catch (Exception e) {
                        log.error("Failed to send report to admin: {}", admin.getEmail(), e);
                    }
                }
            }
            
            if (sentCount > 0) {
                response.put("success", true);
                response.put("message", "Жалоба отправлена администраторам");
                log.info("User {} reported user {} for reason: {}", currentUser.getId(), userId, reason);
            } else {
                response.put("success", false);
                response.put("error", "Не удалось отправить жалобу. Попробуйте позже.");
            }
            
        } catch (Exception e) {
            log.error("Error processing report", e);
            response.put("success", false);
            response.put("error", "Ошибка при обработке жалобы: " + e.getMessage());
        }
        return response;
    }
}

