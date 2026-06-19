package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramAuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${telegram.bot.token:}")
    private String botToken;

    public Map<String, String> normalizeTelegramData(Map<String, Object> raw) {
        Map<String, String> normalized = new HashMap<>();
        if (raw == null) {
            return normalized;
        }
        raw.forEach((key, value) -> {
            if (value != null) {
                normalized.put(key, String.valueOf(value));
            }
        });
        return normalized;
    }

    public Map<String, Object> verifyTelegramLogin(Map<String, Object> rawData) {
        return verifyTelegramLoginNormalized(normalizeTelegramData(rawData));
    }

    /**
     * Verify Telegram login data and return user information
     */
    private Map<String, Object> verifyTelegramLoginNormalized(Map<String, String> telegramData) {
        try {
            if (!verifyTelegramData(telegramData)) {
                return Map.of("success", false, "error", "Недействительные данные Telegram");
            }

            Long telegramId = Long.parseLong(telegramData.get("id"));
            String firstName = telegramData.get("first_name");
            String lastName = telegramData.get("last_name");
            String username = telegramData.get("username");
            String photoUrl = telegramData.get("photo_url");

            User existingUser = userService.findByTelegramId(telegramId);
            if (existingUser != null) {
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
                    if (linkedUser.getTelegramId() == null) {
                        linkedUser.setTelegramId(telegramId);
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            linkedUser.setAvatarUrl(photoUrl);
                        }
                        userRepository.save(linkedUser);
                        return buildSuccessResult(linkedUser, "link");
                    }
                }
            }

            UserWithPassword userWithPassword = createUserFromTelegramData(
                    telegramId, firstName, lastName, username, photoUrl);
            User savedUser = userService.saveUser(userWithPassword.user);

            try {
                emailService.sendTelegramPasswordEmail(savedUser, userWithPassword.temporaryPassword);
            } catch (Exception e) {
                log.error("Failed to send password email", e);
            }

            Map<String, Object> result = buildSuccessResult(savedUser, "register");
            result.put("requiresPasswordSetup", true);
            result.put("temporaryPassword", userWithPassword.temporaryPassword);
            return result;

        } catch (Exception e) {
            log.error("Error during Telegram authentication", e);
            return Map.of("success", false, "error", "Ошибка авторизации: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> linkVerifiedTelegramToUser(Long userId, Map<String, String> telegramData) {
        if (!verifyTelegramData(telegramData)) {
            return Map.of("success", false, "error", "Недействительные данные Telegram");
        }

        Long telegramId = Long.parseLong(telegramData.get("id"));
        String telegramUsername = telegramData.get("username");
        String photoUrl = telegramData.get("photo_url");

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Map.of("success", false, "error", "Пользователь не найден");
        }

        User existingUserWithTelegram = userService.findByTelegramId(telegramId);
        if (existingUserWithTelegram != null && !existingUserWithTelegram.getId().equals(userId)) {
            if (isTelegramStubUser(existingUserWithTelegram)) {
                mergeStubIntoUser(existingUserWithTelegram, user, telegramId, photoUrl);
                log.info("Merged Telegram stub account {} into user {}", telegramId, userId);
            } else {
                return Map.of("success", false,
                        "error", "Этот Telegram уже привязан к другому аккаунту");
            }
        } else {
            user.setTelegramId(telegramId);
            if (photoUrl != null && !photoUrl.isEmpty()) {
                user.setAvatarUrl(photoUrl);
            }
            userService.saveUser(user);
        }

        log.info("Linked Telegram {} (@{}) to user {}", telegramId, telegramUsername, userId);
        return Map.of(
                "success", true,
                "message", "Telegram успешно привязан",
                "username", telegramUsername != null ? telegramUsername : ""
        );
    }

    public boolean linkTelegramToUser(Long userId, Long telegramId, String telegramUsername) {
        Map<String, String> data = new HashMap<>();
        data.put("id", String.valueOf(telegramId));
        data.put("first_name", telegramUsername != null ? telegramUsername : "User");
        data.put("auth_date", String.valueOf(Instant.now().getEpochSecond()));
        data.put("hash", "legacy");
        if (botToken != null && !botToken.isBlank() && !"YOUR_BOT_TOKEN_HERE".equals(botToken)) {
            return false;
        }
        return Boolean.TRUE.equals(linkVerifiedTelegramToUser(userId, data).get("success"));
    }

    public boolean linkTelegramToUserWithPhoto(Long userId, Long telegramId, String telegramUsername, String photoUrl) {
        return linkTelegramToUser(userId, telegramId, telegramUsername);
    }

    @Transactional
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

    private void mergeStubIntoUser(User stub, User target, Long telegramId, String photoUrl) {
        target.setTelegramId(telegramId);
        if ((target.getAvatarUrl() == null || target.getAvatarUrl().isBlank())
                && photoUrl != null && !photoUrl.isEmpty()) {
            target.setAvatarUrl(photoUrl);
        } else if ((target.getAvatarUrl() == null || target.getAvatarUrl().isBlank())
                && stub.getAvatarUrl() != null) {
            target.setAvatarUrl(stub.getAvatarUrl());
        }
        userService.saveUser(target);
        stub.setTelegramId(null);
        userRepository.save(stub);
    }

    private boolean isTelegramStubUser(User user) {
        if (user == null) {
            return false;
        }
        boolean stubEmail = user.getEmail() != null && user.getEmail().startsWith("telegram_")
                && user.getEmail().endsWith("@ruomi.fi");
        boolean stubUsername = user.getUsername() != null && user.getUsername().startsWith("tg_");
        return stubEmail || stubUsername;
    }

    private boolean verifyTelegramData(Map<String, String> telegramData) {
        try {
            String id = telegramData.get("id");
            String firstName = telegramData.get("first_name");
            String authDate = telegramData.get("auth_date");
            String hash = telegramData.get("hash");

            if (id == null || firstName == null || authDate == null || hash == null) {
                return false;
            }

            long authTimestamp = Long.parseLong(authDate);
            if (Instant.now().getEpochSecond() - authTimestamp > 86400) {
                log.warn("Telegram auth_date expired");
                return false;
            }

            if (botToken == null || botToken.isBlank() || "YOUR_BOT_TOKEN_HERE".equals(botToken)) {
                log.warn("Telegram bot token not configured — hash check skipped");
                return true;
            }

            TreeMap<String, String> sorted = new TreeMap<>();
            telegramData.forEach((key, value) -> {
                if (!"hash".equals(key) && value != null) {
                    sorted.put(key, value);
                }
            });

            StringBuilder dataCheck = new StringBuilder();
            sorted.forEach((key, value) -> {
                if (dataCheck.length() > 0) {
                    dataCheck.append('\n');
                }
                dataCheck.append(key).append('=').append(value);
            });

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] secretKey = digest.digest(botToken.getBytes(StandardCharsets.UTF_8));

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] calculated = mac.doFinal(dataCheck.toString().getBytes(StandardCharsets.UTF_8));

            return bytesToHex(calculated).equalsIgnoreCase(hash);
        } catch (Exception e) {
            log.error("Error verifying Telegram data", e);
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private UserWithPassword createUserFromTelegramData(Long telegramId, String firstName, String lastName,
                                                        String username, String photoUrl) {
        User user = new User();
        String finalUsername = resolveUniqueUsername(username, telegramId);
        String randomPassword = generateReadablePassword();

        user.setUsername(finalUsername);
        user.setEmail("telegram_" + telegramId + "@ruomi.fi");
        user.setPassword(passwordEncoder.encode(randomPassword));
        user.setFirstName(firstName != null ? firstName : "Telegram");
        user.setLastName(lastName != null ? lastName : "User");
        user.setTelegramId(telegramId);
        if (photoUrl != null && !photoUrl.isEmpty()) {
            user.setAvatarUrl(photoUrl);
        }
        user.setRole(User.UserRole.USER);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());

        return new UserWithPassword(user, randomPassword);
    }

    private String resolveUniqueUsername(String username, Long telegramId) {
        String base = (username != null && !username.isEmpty()) ? username : "tg_" + telegramId;
        if (userRepository.findByUsername(base).isEmpty()) {
            return base;
        }
        for (int i = 1; i <= 100; i++) {
            String candidate = base + "_" + i;
            if (userRepository.findByUsername(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "tg_" + telegramId + "_" + System.currentTimeMillis();
    }

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

    private static class UserWithPassword {
        final User user;
        final String temporaryPassword;

        UserWithPassword(User user, String temporaryPassword) {
            this.user = user;
            this.temporaryPassword = temporaryPassword;
        }
    }
}
