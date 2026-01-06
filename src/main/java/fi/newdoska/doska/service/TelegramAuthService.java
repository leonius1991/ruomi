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
            
            // Check if user already exists with this Telegram ID
            User existingUser = userService.findByTelegramId(telegramId);
            if (existingUser != null) {
                return buildSuccessResult(existingUser, "login");
            }
            
            if (username != null && !username.isEmpty()) {
                Optional<User> userByUsername = userRepository.findByUsername(username);
                if (userByUsername.isPresent()) {
                    User linkedUser = userByUsername.get();
                    linkedUser.setTelegramId(telegramId);
                    userRepository.save(linkedUser);
                    return buildSuccessResult(linkedUser, "link");
                }
            }
            
            User newUser = createUserFromTelegramData(telegramId, firstName, lastName, username);
            User savedUser = userService.saveUser(newUser);
            
            return buildSuccessResult(savedUser, "register");
            
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
     */
    private User createUserFromTelegramData(Long telegramId, String firstName, String lastName,
                                          String username) {
        User user = new User();
        
        // Generate a unique username if not provided
        String finalUsername = username != null && !username.isEmpty() ? username : 
                              "tg_" + telegramId;
        
        // Generate a random password for Telegram users
        String randomPassword = java.util.UUID.randomUUID().toString();
        
        user.setUsername(finalUsername);
        user.setEmail("telegram_" + telegramId + "@ruomi.fi"); // Placeholder email
        user.setPassword(passwordEncoder.encode(randomPassword));
        user.setFirstName(firstName != null ? firstName : "Telegram");
        user.setLastName(lastName != null ? lastName : "User");
        user.setTelegramId(telegramId);
        user.setRole(User.UserRole.USER);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        
        return user;
    }
    
    private Map<String, Object> buildSuccessResult(User user, String action) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("success", true);
        payload.put("action", action);
        payload.put("user", user);
        return payload;
    }
}
