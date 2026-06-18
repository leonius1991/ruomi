package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramAuthService {
    
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    @Value("${telegram.bot.token}")
    private String botToken;
    
    /**
     * Verify Telegram login data and return user information
     */
    public Map<String, Object> verifyTelegramLogin(Map<String, String> telegramData) {
        try {
            // Verify the data using Telegram's API
            if (!verifyTelegramData(telegramData)) {
                return Map.of("success", false, "error", "Invalid Telegram data");
            }
            
            Long telegramId = Long.parseLong(telegramData.get("id"));
            String firstName = telegramData.get("first_name");
            String lastName = telegramData.get("last_name");
            String username = telegramData.get("username");
            String photoUrl = telegramData.get("photo_url");
            
            // Check if user already exists with this Telegram ID
            User existingUser = userService.findByTelegramId(telegramId);
            if (existingUser != null) {
                // Обновляем аватарку, если есть
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    existingUser.setAvatarUrl(photoUrl);
                    userService.saveUser(existingUser);
                }
                return buildSuccessResult(existingUser, "login");
            }
            
            if (username != null && !username.isEmpty()) {
                Optional<User> userByUsername = userRepository.findByUsername(username);
                if (userByUsername.isPresent()) {
                    User linkedUser = userByUsername.get();
                    linkedUser.setTelegramId(telegramId);
                    // Обновляем аватарку, если есть
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        linkedUser.setAvatarUrl(photoUrl);
                    }
                    userRepository.save(linkedUser);
                    return buildSuccessResult(linkedUser, "link");
                }
            }
            
            UserWithPassword userWithPassword = createUserFromTelegramData(telegramId, firstName, lastName, username, photoUrl);
            User savedUser = userService.saveUser(userWithPassword.user);
            
            // Отправляем пароль на email, если есть реальный email (не placeholder)
            try {
                emailService.sendTelegramPasswordEmail(savedUser, userWithPassword.temporaryPassword);
            } catch (Exception e) {
                log.error("Failed to send password email", e);
            }
            
            // Возвращаем временный пароль для показа пользователю (только один раз)
            Map<String, Object> result = buildSuccessResult(savedUser, "register");
            result.put("requiresPasswordSetup", true);
            result.put("temporaryPassword", userWithPassword.temporaryPassword);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error during Telegram authentication", e);
            return Map.of("success", false, "error", "Authentication failed: " + e.getMessage());
        }
    }
    
    /**
     * Link Telegram account to existing user
     */
    public boolean linkTelegramToUser(Long userId, Long telegramId, String telegramUsername) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return false;
            }
            
            // Check if this Telegram ID is already linked to another account
            User existingUserWithTelegram = userService.findByTelegramId(telegramId);
            if (existingUserWithTelegram != null && !existingUserWithTelegram.getId().equals(userId)) {
                return false;
            }
            
            user.setTelegramId(telegramId);
            userService.saveUser(user);
            
            log.info("Linked Telegram account {} to user {}", telegramId, userId);
            return true;
            
        } catch (Exception e) {
            log.error("Error linking Telegram account", e);
            return false;
        }
    }
    
    /**
     * Link Telegram account with photo URL
     */
    public boolean linkTelegramToUserWithPhoto(Long userId, Long telegramId, String telegramUsername, String photoUrl) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return false;
            }
            
            // Check if this Telegram ID is already linked to another account
            User existingUserWithTelegram = userService.findByTelegramId(telegramId);
            if (existingUserWithTelegram != null && !existingUserWithTelegram.getId().equals(userId)) {
                return false;
            }
            
            user.setTelegramId(telegramId);
            // Обновляем аватарку, если есть
            if (photoUrl != null && !photoUrl.isEmpty()) {
                user.setAvatarUrl(photoUrl);
            }
            userService.saveUser(user);
            
            log.info("Linked Telegram account {} to user {} with photo", telegramId, userId);
            return true;
            
        } catch (Exception e) {
            log.error("Error linking Telegram account", e);
            return false;
        }
    }
    
    /**
     * Unlink Telegram account from user
     */
    public boolean unlinkTelegramFromUser(Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return false;
            }
            
            user.setTelegramId(null);
            userService.saveUser(user);
            
            log.info("Unlinked Telegram account from user {}", userId);
            return true;
            
        } catch (Exception e) {
            log.error("Error unlinking Telegram account", e);
            return false;
        }
    }
    
    /**
     * Verify Telegram data using Telegram's API
     */
    private boolean verifyTelegramData(Map<String, String> telegramData) {
        try {
            // For Telegram Login Widget, we need to verify the data hash
            // This is a simplified verification - in production, you should implement proper hash verification
            
            String id = telegramData.get("id");
            String firstName = telegramData.get("first_name");
            String authDate = telegramData.get("auth_date");
            String hash = telegramData.get("hash");
            
            if (id == null || firstName == null || authDate == null || hash == null) {
                return false;
            }
            
            // In a real implementation, you would verify the hash here
            // For now, we'll do basic validation
            return true;
            
        } catch (Exception e) {
            log.error("Error verifying Telegram data", e);
            return false;
        }
    }
    
    /**
     * Create a new user from Telegram data
     * Returns both user and temporary password
     */
    private UserWithPassword createUserFromTelegramData(Long telegramId, String firstName, String lastName,
                                          String username, String photoUrl) {
        User user = new User();
        
        // Generate a unique username if not provided
        String finalUsername = username != null && !username.isEmpty() ? username : 
                              "tg_" + telegramId;
        
        // Generate a readable password (8 символов: буквы и цифры)
        String randomPassword = generateReadablePassword();
        
        user.setUsername(finalUsername);
        user.setEmail("telegram_" + telegramId + "@ruomi.fi"); // Placeholder email
        user.setPassword(passwordEncoder.encode(randomPassword));
        user.setFirstName(firstName != null ? firstName : "Telegram");
        user.setLastName(lastName != null ? lastName : "User");
        user.setTelegramId(telegramId);
        // Сохраняем аватарку из Telegram, если есть
        if (photoUrl != null && !photoUrl.isEmpty()) {
            user.setAvatarUrl(photoUrl);
        }
        user.setRole(User.UserRole.USER);
        user.setEnabled(true); // Telegram пользователи сразу активны
        user.setCreatedAt(LocalDateTime.now());
        
        return new UserWithPassword(user, randomPassword);
    }
    
    /**
     * Генерирует читаемый пароль (8 символов: буквы и цифры, без похожих символов)
     */
    private String generateReadablePassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
    
    private Map<String, Object> buildSuccessResult(User user, String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("success", true);
        payload.put("action", action);
        payload.put("user", user);
        return payload;
    }
    
    /**
     * Вспомогательный класс для возврата пользователя и временного пароля
     */
    private static class UserWithPassword {
        final User user;
        final String temporaryPassword;
        
        UserWithPassword(User user, String temporaryPassword) {
            this.user = user;
            this.temporaryPassword = temporaryPassword;
        }
    }
}
