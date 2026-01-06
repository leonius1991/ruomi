package fi.newdoska.doska.config;

import fi.newdoska.doska.telegram.VfinkeTelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import jakarta.annotation.PostConstruct;

@Configuration
public class TelegramBotAutoConfiguration {
    
    @Autowired
    private VfinkeTelegramBot telegramBot;
    
    @PostConstruct
    public void registerBot() {
        // Проверяем, настроен ли бот
        String botToken = telegramBot.getBotToken();
        if (botToken == null || botToken.isEmpty() || botToken.equals("YOUR_BOT_TOKEN_HERE")) {
            System.out.println("⚠️ Telegram bot not configured. Skipping registration.");
            return;
        }
        
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBot);
            System.out.println("✅ Telegram bot registered successfully!");
        } catch (TelegramApiException e) {
            // Не критичная ошибка - бот может быть не настроен
            if (e.getMessage() != null && e.getMessage().contains("webhook")) {
                System.out.println("⚠️ Telegram bot webhook warning (non-critical): " + e.getMessage());
            } else {
                System.err.println("❌ Failed to register Telegram bot: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("❌ Unexpected error registering Telegram bot: " + e.getMessage());
        }
    }
}



