package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${analytics.alert.email:}")
    private String analyticsAlertEmail;
    
    @Value("${server.servlet.context-path:/}")
    private String contextPath;
    
    public void sendVerificationEmail(User user) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("Cannot send verification email: user email is empty for user {}", user.getUsername());
            return;
        }
        
        if (fromEmail == null || fromEmail.isEmpty()) {
            log.error("Cannot send verification email: fromEmail is not configured in application.properties");
            return;
        }
        
        try {
            // Исправляем двойной слеш
            String baseUrl = "https://ruomi.fi";
            String verifyPath = contextPath.equals("/") ? "/verify" : contextPath + "/verify";
            String verificationUrl = baseUrl + verifyPath + "?token=" + user.getVerificationToken();
            
            // Используем HTML шаблон
            String htmlContent = String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<div style='background: linear-gradient(135deg, #1a1a2e 0%%, #16213e 100%%); color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;'>" +
                "<h1 style='margin: 0; font-size: 24px;'>ruomi.fi</h1>" +
                "</div>" +
                "<div style='background: #f8fafc; padding: 30px; border-radius: 0 0 8px 8px;'>" +
                "<p style='font-size: 16px;'>Здравствуйте, <strong>%s</strong>!</p>" +
                "<p style='font-size: 16px;'>Спасибо за регистрацию на сайте ruomi.fi!</p>" +
                "<p style='font-size: 16px;'>Для подтверждения вашего аккаунта перейдите по ссылке:</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='%s' style='display: inline-block; background: #0ea5e9; color: white; padding: 12px 30px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px;'>Подтвердить аккаунт</a>" +
                "</div>" +
                "<p style='font-size: 14px; color: #666; margin-top: 20px;'>Или скопируйте и вставьте эту ссылку в браузер:</p>" +
                "<p style='font-size: 12px; color: #999; word-break: break-all;'>%s</p>" +
                "<p style='font-size: 14px; color: #666;'>Ссылка действительна в течение 24 часов.</p>" +
                "<p style='font-size: 14px; color: #666; margin-top: 30px;'>С уважением,<br>Команда ruomi.fi</p>" +
                "</div>" +
                "</body>" +
                "</html>",
                user.getFirstName() != null ? user.getFirstName() : "Пользователь",
                verificationUrl,
                verificationUrl
            );
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Подтверждение регистрации - ruomi.fi");
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            log.info("Verification email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}. Error: {}", user.getEmail(), e.getMessage(), e);
            // Не пробрасываем исключение, чтобы не прерывать процесс регистрации
        }
    }
    
    public void sendPasswordResetEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Сброс пароля - ruomi.fi");
            
            String resetUrl = "https://ruomi.fi" + contextPath + "/reset-password?token=" + user.getVerificationToken();
            
            message.setText(String.format(
                "Здравствуйте, %s!\n\n" +
                "Вы запросили сброс пароля для вашего аккаунта на сайте ruomi.fi.\n\n" +
                "Для установки нового пароля перейдите по ссылке:\n" +
                "%s\n\n" +
                "Ссылка действительна в течение 1 часа.\n\n" +
                "Если вы не запрашивали сброс пароля, проигнорируйте это письмо.\n\n" +
                "С уважением,\n" +
                "Команда ruomi.fi",
                user.getFirstName(),
                resetUrl
            ));
            
            mailSender.send(message);
            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
        }
    }
    
    public void sendWelcomeEmail(User user) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("Cannot send welcome email: user email is empty for user {}", user.getUsername());
            return;
        }
        
        if (fromEmail == null || fromEmail.isEmpty()) {
            log.error("Cannot send welcome email: fromEmail is not configured in application.properties");
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Добро пожаловать на ruomi.fi!");
            
            message.setText(String.format(
                "Здравствуйте, %s!\n\n" +
                "Добро пожаловать на ruomi.fi - современную доску объявлений для русскоязычного населения в Финляндии!\n\n" +
                "Ваш аккаунт успешно подтвержден. Теперь вы можете:\n" +
                "- Размещать объявления\n" +
                "- Просматривать объявления других пользователей\n" +
                "- Использовать премиум функции\n" +
                "- Общаться с другими пользователями\n\n" +
                "Если у вас есть вопросы, не стесняйтесь обращаться в службу поддержки.\n\n" +
                "С уважением,\n" +
                "Команда ruomi.fi",
                user.getFirstName() != null ? user.getFirstName() : "Пользователь"
            ));
            
            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}. Error: {}", user.getEmail(), e.getMessage(), e);
        }
    }
    
    public void sendAdvertisementApprovedEmail(User user, String advertisementTitle) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Ваше объявление одобрено - ruomi.fi");
            
            message.setText(String.format(
                "Здравствуйте, %s!\n\n" +
                "Ваше объявление \"%s\" было одобрено и опубликовано на сайте ruomi.fi.\n\n" +
                "Теперь другие пользователи могут видеть ваше объявление.\n\n" +
                "С уважением,\n" +
                "Команда ruomi.fi",
                user.getFirstName(),
                advertisementTitle
            ));
            
            mailSender.send(message);
            log.info("Advertisement approved email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send advertisement approved email to: {}", user.getEmail(), e);
        }
    }
    
    public void sendAdvertisementRejectedEmail(User user, String advertisementTitle, String reason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Ваше объявление отклонено - ruomi.fi");
            
            message.setText(String.format(
                "Здравствуйте, %s!\n\n" +
                "Ваше объявление \"%s\" было отклонено модератором.\n\n" +
                "Причина: %s\n\n" +
                "Пожалуйста, исправьте указанные замечания и попробуйте разместить объявление снова.\n\n" +
                "С уважением,\n" +
                "Команда ruomi.fi",
                user.getFirstName(),
                advertisementTitle,
                reason
            ));
            
            mailSender.send(message);
            log.info("Advertisement rejected email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send advertisement rejected email to: {}", user.getEmail(), e);
        }
    }

    public void sendSearchAlert(String recipient, String term, String category, String city) {
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipient);
            message.setSubject("Популярный поиск без результатов — ruomi.fi");

            String body = String.format(
                    "Запрос \"%s\" не дал результатов.%nКатегория: %s%nГород: %s%n" +
                    "Добавьте подходящие объявления или предложите пользователю альтернативу.",
                    term,
                    category != null ? category : "-",
                    city != null ? city : "-"
            );
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send search alert email for query {}", term, e);
        }
    }
    
    /**
     * Отправка пароля для Telegram пользователя
     */
    public void sendTelegramPasswordEmail(User user, String temporaryPassword) {
        try {
            // Проверяем, что это не placeholder email
            if (user.getEmail() == null || user.getEmail().isEmpty() || 
                user.getEmail().startsWith("telegram_") && user.getEmail().endsWith("@ruomi.fi")) {
                // Placeholder email - не отправляем
                return;
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Ваш пароль для входа на ruomi.fi");
            
            String loginUrl = "https://ruomi.fi" + contextPath + "/login";
            
            message.setText(String.format(
                "Здравствуйте, %s!\n\n" +
                "Вы зарегистрировались на ruomi.fi через Telegram.\n\n" +
                "Ваш пароль для входа через обычную форму:\n" +
                "%s\n\n" +
                "Рекомендуем:\n" +
                "1. Сохраните этот пароль в безопасном месте\n" +
                "2. Или установите свой пароль в настройках профиля\n" +
                "3. Вы также можете продолжать входить через Telegram\n\n" +
                "Войти на сайт: %s\n\n" +
                "С уважением,\n" +
                "Команда ruomi.fi",
                user.getFirstName(),
                temporaryPassword,
                loginUrl
            ));
            
            mailSender.send(message);
            log.info("Telegram password email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send Telegram password email to: {}", user.getEmail(), e);
        }
    }
    
    /**
     * Отправка уведомления о новом личном сообщении
     */
    public void sendPrivateMessageNotification(User recipient, User sender, String messagePreview) {
        try {
            if (recipient.getEmail() == null || recipient.getEmail().isEmpty()) {
                return;
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipient.getEmail());
            message.setSubject("Новое сообщение от " + sender.getFirstName() + " " + sender.getLastName() + " - ruomi.fi");
            
            String messageUrl = "https://ruomi.fi" + contextPath + "/messages";
            String preview = messagePreview != null && messagePreview.length() > 150 
                ? messagePreview.substring(0, 150) + "..." 
                : messagePreview;
            
            message.setText(String.format(
                "Здравствуйте, %s!\n\n" +
                "Вы получили новое личное сообщение от %s %s (@%s).\n\n" +
                "Сообщение:\n" +
                "%s\n\n" +
                "Чтобы прочитать и ответить на сообщение, перейдите по ссылке:\n" +
                "%s\n\n" +
                "С уважением,\n" +
                "Команда ruomi.fi",
                recipient.getFirstName(),
                sender.getFirstName(),
                sender.getLastName(),
                sender.getUsername(),
                preview != null ? preview : "[Сообщение]",
                messageUrl
            ));
            
            mailSender.send(message);
            log.info("Private message notification email sent to: {}", recipient.getEmail());
        } catch (Exception e) {
            log.error("Failed to send private message notification email to: {}", recipient.getEmail(), e);
        }
    }
    
    /**
     * Отправка тестового письма с выбранным шаблоном
     * @param testEmail Email адрес для отправки
     * @param templateType Тип шаблона (verification, welcome, password_reset, ad_approved, ad_rejected, telegram_password, private_message, search_alert)
     * @return true если отправка успешна, false в случае ошибки
     */
    public boolean sendTestEmail(String testEmail, String templateType) {
        try {
            // Создаем тестового пользователя
            User testUser = new User();
            testUser.setEmail(testEmail);
            testUser.setFirstName("Тестовый");
            testUser.setLastName("Пользователь");
            testUser.setUsername("test_user");
            testUser.setVerificationToken("test_token_12345");
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(testEmail);
            
            switch (templateType.toLowerCase()) {
                case "verification":
                    message.setSubject("Тест: Подтверждение регистрации - ruomi.fi");
                    String verificationUrl = "https://ruomi.fi" + contextPath + "/verify?token=test_token_12345";
                    message.setText(String.format(
                        "Здравствуйте, %s!\n\n" +
                        "Спасибо за регистрацию на сайте ruomi.fi!\n\n" +
                        "Для подтверждения вашего аккаунта перейдите по ссылке:\n" +
                        "%s\n\n" +
                        "Ссылка действительна в течение 24 часов.\n\n" +
                        "С уважением,\n" +
                        "Команда ruomi.fi",
                        testUser.getFirstName(),
                        verificationUrl
                    ));
                    break;
                    
                case "welcome":
                    message.setSubject("Тест: Добро пожаловать на ruomi.fi!");
                    message.setText(String.format(
                        "Здравствуйте, %s!\n\n" +
                        "Добро пожаловать на ruomi.fi - современную доску объявлений для русскоязычного населения в Финляндии!\n\n" +
                        "Ваш аккаунт успешно подтвержден. Теперь вы можете:\n" +
                        "- Размещать объявления\n" +
                        "- Просматривать объявления других пользователей\n" +
                        "- Использовать премиум функции\n" +
                        "- Общаться с другими пользователями\n\n" +
                        "Если у вас есть вопросы, не стесняйтесь обращаться в службу поддержки.\n\n" +
                        "С уважением,\n" +
                        "Команда ruomi.fi",
                        testUser.getFirstName()
                    ));
                    break;
                    
                case "password_reset":
                    message.setSubject("Тест: Сброс пароля - ruomi.fi");
                    String resetUrl = "https://ruomi.fi" + contextPath + "/reset-password?token=test_token_12345";
                    message.setText(String.format(
                        "Здравствуйте, %s!\n\n" +
                        "Вы запросили сброс пароля для вашего аккаунта на сайте ruomi.fi.\n\n" +
                        "Для установки нового пароля перейдите по ссылке:\n" +
                        "%s\n\n" +
                        "Ссылка действительна в течение 1 часа.\n\n" +
                        "Если вы не запрашивали сброс пароля, проигнорируйте это письмо.\n\n" +
                        "С уважением,\n" +
                        "Команда ruomi.fi",
                        testUser.getFirstName(),
                        resetUrl
                    ));
                    break;
                    
                case "ad_approved":
                    message.setSubject("Тест: Ваше объявление одобрено - ruomi.fi");
                    message.setText(String.format(
                        "Здравствуйте, %s!\n\n" +
                        "Ваше объявление \"Тестовое объявление\" было одобрено и опубликовано на сайте ruomi.fi.\n\n" +
                        "Теперь другие пользователи могут видеть ваше объявление.\n\n" +
                        "С уважением,\n" +
                        "Команда ruomi.fi",
                        testUser.getFirstName()
                    ));
                    break;
                    
                case "ad_rejected":
                    message.setSubject("Тест: Ваше объявление отклонено - ruomi.fi");
                    message.setText(String.format(
                        "Здравствуйте, %s!\n\n" +
                        "Ваше объявление \"Тестовое объявление\" было отклонено модератором.\n\n" +
                        "Причина: Тестовая причина отклонения\n\n" +
                        "Пожалуйста, исправьте указанные замечания и попробуйте разместить объявление снова.\n\n" +
                        "С уважением,\n" +
                        "Команда ruomi.fi",
                        testUser.getFirstName()
                    ));
                    break;
                    
                case "telegram_password":
                    message.setSubject("Тест: Ваш пароль для входа на ruomi.fi");
                    String loginUrl = "https://ruomi.fi" + contextPath + "/login";
                    message.setText(String.format(
                        "Здравствуйте, %s!\n\n" +
                        "Вы зарегистрировались на ruomi.fi через Telegram.\n\n" +
                        "Ваш пароль для входа через обычную форму:\n" +
                        "TestPass123\n\n" +
                        "Рекомендуем:\n" +
                        "1. Сохраните этот пароль в безопасном месте\n" +
                        "2. Или установите свой пароль в настройках профиля\n" +
                        "3. Вы также можете продолжать входить через Telegram\n\n" +
                        "Войти на сайт: %s\n\n" +
                        "С уважением,\n" +
                        "Команда ruomi.fi",
                        testUser.getFirstName(),
                        loginUrl
                    ));
                    break;
                    
                case "private_message":
                    message.setSubject("Тест: Новое сообщение от Тестовый Отправитель - ruomi.fi");
                    String messageUrl = "https://ruomi.fi" + contextPath + "/messages";
                    message.setText(String.format(
                        "Здравствуйте, %s!\n\n" +
                        "Вы получили новое личное сообщение от Тестовый Отправитель (@test_sender).\n\n" +
                        "Сообщение:\n" +
                        "Это тестовое сообщение для проверки работы почтовой системы.\n\n" +
                        "Чтобы прочитать и ответить на сообщение, перейдите по ссылке:\n" +
                        "%s\n\n" +
                        "С уважением,\n" +
                        "Команда ruomi.fi",
                        testUser.getFirstName(),
                        messageUrl
                    ));
                    break;
                    
                case "search_alert":
                    message.setSubject("Тест: Популярный поиск без результатов — ruomi.fi");
                    message.setText(String.format(
                        "Запрос \"тестовый запрос\" не дал результатов.\n" +
                        "Категория: Тестовая категория\n" +
                        "Город: Хельсинки\n" +
                        "Добавьте подходящие объявления или предложите пользователю альтернативу."
                    ));
                    break;
                    
                default:
                    log.error("Unknown email template type: {}", templateType);
                    return false;
            }
            
            mailSender.send(message);
            log.info("Test email sent successfully to: {} with template: {}", testEmail, templateType);
            return true;
        } catch (Exception e) {
            log.error("Failed to send test email to: {} with template: {}", testEmail, templateType, e);
            throw e;
        }
    }
    
    /**
     * Отправка уведомления модераторам/администраторам о новом объявлении, требующем модерации
     */
    public void sendModerationNotification(User moderator, Advertisement advertisement) {
        try {
            if (moderator.getEmail() == null || moderator.getEmail().isEmpty()) {
                return;
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(moderator.getEmail());
            message.setSubject("Новое объявление требует модерации - ruomi.fi");
            
            String moderationUrl = "https://ruomi.fi" + contextPath + "/moderator/dashboard";
            
            message.setText(String.format(
                "Здравствуйте, %s!\n\n" +
                "Новое объявление ожидает модерации:\n\n" +
                "Заголовок: %s\n" +
                "Категория: %s\n" +
                "Автор: %s %s (@%s)\n" +
                "Цена: %s\n" +
                "Город: %s\n\n" +
                "Описание:\n%s\n\n" +
                "Для модерации перейдите по ссылке:\n" +
                "%s\n\n" +
                "С уважением,\n" +
                "Команда ruomi.fi",
                moderator.getFirstName(),
                advertisement.getTitle(),
                advertisement.getCategory().getDisplayName(),
                advertisement.getUser().getFirstName(),
                advertisement.getUser().getLastName(),
                advertisement.getUser().getUsername(),
                advertisement.getPrice() != null ? advertisement.getPrice() + " €" : "Не указана",
                advertisement.getCity() != null ? advertisement.getCity() : "Не указан",
                advertisement.getDescription().length() > 200 
                    ? advertisement.getDescription().substring(0, 200) + "..." 
                    : advertisement.getDescription(),
                moderationUrl
            ));
            
            mailSender.send(message);
            log.info("Moderation notification email sent to: {}", moderator.getEmail());
        } catch (Exception e) {
            log.error("Failed to send moderation notification email to: {}", moderator.getEmail(), e);
        }
    }
} 