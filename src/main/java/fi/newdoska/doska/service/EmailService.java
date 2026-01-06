package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    
    @Value("${server.servlet.context-path}")
    private String contextPath;
    
    public void sendVerificationEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Подтверждение регистрации - ruomi.fi");
            
            String verificationUrl = "http://localhost:8080" + contextPath + "/verify?token=" + user.getVerificationToken();
            
            message.setText(String.format(
                "Здравствуйте, %s!\n\n" +
                "Спасибо за регистрацию на сайте ruomi.fi!\n\n" +
                "Для подтверждения вашего аккаунта перейдите по ссылке:\n" +
                "%s\n\n" +
                "Ссылка действительна в течение 24 часов.\n\n" +
                "С уважением,\n" +
                "Команда ruomi.fi",
                user.getFirstName(),
                verificationUrl
            ));
            
            mailSender.send(message);
            log.info("Verification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
        }
    }
    
    public void sendPasswordResetEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Сброс пароля - ruomi.fi");
            
            String resetUrl = "http://localhost:8080" + contextPath + "/reset-password?token=" + user.getVerificationToken();
            
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
                user.getFirstName()
            ));
            
            mailSender.send(message);
            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
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
} 