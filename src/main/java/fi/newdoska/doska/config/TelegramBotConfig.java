package fi.newdoska.doska.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfig {
    
    @Value("${telegram.bot.username}")
    private String botUsername;
    
    @Value("${telegram.bot.token}")
    private String botToken;
    
    public String getBotUsername() {
        return botUsername;
    }
    
    public String getBotToken() {
        return botToken;
    }
}



