package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.BroadcastMessage;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.BroadcastMessageRepository;
import fi.newdoska.doska.repository.UserRepository;
import fi.newdoska.doska.telegram.VfinkeTelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BroadcastMessageService {
    
    private final BroadcastMessageRepository broadcastRepository;
    private final UserRepository userRepository;
    
    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private VfinkeTelegramBot telegramBot;
    
    public BroadcastMessage createBroadcast(User createdBy, String content) {
        BroadcastMessage broadcast = new BroadcastMessage();
        broadcast.setCreatedBy(createdBy);
        broadcast.setContent(content);
        broadcast.setCreatedAt(LocalDateTime.now());
        broadcast.setSent(false);
        broadcast.setTotalRecipients(0);
        broadcast.setSuccessfulSends(0);
        broadcast.setFailedSends(0);
        
        return broadcastRepository.save(broadcast);
    }
    
    public void sendBroadcast(Long broadcastId) {
        BroadcastMessage broadcast = broadcastRepository.findById(broadcastId)
            .orElseThrow(() -> new IllegalArgumentException("Рассылка не найдена"));
        
        if (broadcast.getSent()) {
            throw new IllegalStateException("Рассылка уже отправлена");
        }
        
        if (telegramBot == null) {
            throw new IllegalStateException("Telegram бот не настроен");
        }
        
        List<User> users = userRepository.findAll();
        int total = 0;
        int success = 0;
        int failed = 0;
        
        for (User user : users) {
            if (user.getTelegramId() != null) {
                total++;
                try {
                    telegramBot.sendMessage(user.getTelegramId(), broadcast.getContent());
                    success++;
                } catch (Exception e) {
                    log.error("Failed to send broadcast to user " + user.getId(), e);
                    failed++;
                }
            }
        }
        
        broadcast.setSent(true);
        broadcast.setSentAt(LocalDateTime.now());
        broadcast.setTotalRecipients(total);
        broadcast.setSuccessfulSends(success);
        broadcast.setFailedSends(failed);
        
        broadcastRepository.save(broadcast);
    }
    
    public List<BroadcastMessage> getAllBroadcasts() {
        return broadcastRepository.findAllByOrderByCreatedAtDesc();
    }
    
    public List<BroadcastMessage> getUnsentBroadcasts() {
        return broadcastRepository.findBySentOrderByCreatedAtDesc(false);
    }
    
    public BroadcastMessage getBroadcast(Long id) {
        return broadcastRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Рассылка не найдена"));
    }
}

