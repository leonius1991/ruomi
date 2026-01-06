package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.telegram.VfinkeTelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {
    
    private final EmailService emailService;
    private VfinkeTelegramBot telegramBot;
    
    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }
    
    @Autowired(required = false)
    @Lazy
    public void setTelegramBot(VfinkeTelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }
    
    public void notifyNewAdvertisement(User user, String advertisementTitle) {
        // Email уведомление
        if (user.getEmail() != null) {
            try {
                emailService.sendAdvertisementApprovedEmail(user, advertisementTitle);
            } catch (Exception e) {
                log.error("Failed to send email notification", e);
            }
        }
        
        // Telegram уведомление
        if (user.getTelegramId() != null && telegramBot != null) {
            try {
                telegramBot.sendMessage(user.getTelegramId(), 
                    "✅ Ваше объявление \"" + advertisementTitle + "\" было опубликовано!");
            } catch (Exception e) {
                log.error("Failed to send Telegram notification", e);
            }
        }
    }
    
    public void notifyAdvertisementRejected(User user, String advertisementTitle, String reason) {
        if (user.getEmail() != null) {
            try {
                emailService.sendAdvertisementRejectedEmail(user, advertisementTitle, reason);
            } catch (Exception e) {
                log.error("Failed to send email notification", e);
            }
        }
        
        if (user.getTelegramId() != null && telegramBot != null) {
            try {
                telegramBot.sendMessage(user.getTelegramId(), 
                    "❌ Ваше объявление \"" + advertisementTitle + "\" было отклонено. Причина: " + reason);
            } catch (Exception e) {
                log.error("Failed to send Telegram notification", e);
            }
        }
    }
}
