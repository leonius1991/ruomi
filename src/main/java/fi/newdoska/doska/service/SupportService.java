package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportService {
    
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    
    @Transactional(readOnly = true)
    public void sendSupportMessage(String fromEmail, String subject, String message, User sender) {
        // Получаем всех админов, супер админов и модераторов
        List<User> admins = userRepository.findAll().stream()
                .filter(user -> user.getRole() == User.UserRole.SUPER_ADMIN || 
                               user.getRole() == User.UserRole.ADMIN || 
                               user.getRole() == User.UserRole.MODERATOR)
                .collect(Collectors.toList());
        
        if (admins.isEmpty()) {
            log.warn("No admins/moderators found to send support message to");
            return;
        }
        
        // Формируем сообщение
        String emailContent = String.format(
            "Новое сообщение в поддержку от %s\n\n" +
            "Email отправителя: %s\n" +
            "Пользователь: %s\n" +
            "Тема: %s\n\n" +
            "Сообщение:\n%s\n\n" +
            "---\n" +
            "Это автоматическое уведомление от ruomi.fi",
            sender != null ? sender.getUsername() : "Анонимный пользователь",
            fromEmail,
            sender != null ? String.format("%s %s (ID: %d)", 
                sender.getFirstName() != null ? sender.getFirstName() : "", 
                sender.getLastName() != null ? sender.getLastName() : "",
                sender.getId()) : "Не зарегистрирован",
            subject != null ? subject : "Без темы",
            message
        );
        
        // Отправляем письмо каждому админу/модератору
        int sentCount = 0;
        for (User admin : admins) {
            if (admin.getEmail() != null && !admin.getEmail().trim().isEmpty()) {
                try {
                    SimpleMailMessage mailMessage = new SimpleMailMessage();
                    mailMessage.setTo(admin.getEmail());
                    mailMessage.setSubject("[ruomi.fi Поддержка] " + (subject != null ? subject : "Новое сообщение"));
                    mailMessage.setText(emailContent);
                    mailMessage.setFrom("noreply@ruomi.fi");
                    
                    mailSender.send(mailMessage);
                    sentCount++;
                    log.info("Support message sent successfully to admin: {}", admin.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send support message to admin: {} - Error: {}", admin.getEmail(), e.getMessage(), e);
                }
            } else {
                log.warn("Admin {} has no email address", admin.getUsername());
            }
        }
        
        if (sentCount == 0) {
            log.error("No support messages were sent. Total admins: {}, Admins with email: {}", 
                admins.size(), 
                admins.stream().filter(a -> a.getEmail() != null && !a.getEmail().trim().isEmpty()).count());
        } else {
            log.info("Support messages sent to {} out of {} admins/moderators", sentCount, admins.size());
        }
    }
}

