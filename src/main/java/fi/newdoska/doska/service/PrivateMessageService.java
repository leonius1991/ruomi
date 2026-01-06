package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.PrivateMessage;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.PrivateMessageRepository;
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
public class PrivateMessageService {
    
    private final PrivateMessageRepository messageRepository;
    
    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private VfinkeTelegramBot telegramBot;
    
    public PrivateMessage sendMessage(User sender, User recipient, String content) {
        PrivateMessage message = new PrivateMessage();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());
        message.setRead(false);
        
        PrivateMessage saved = messageRepository.save(message);
        
        // Отправляем уведомление в Telegram, если получатель подключен
        if (recipient.getTelegramId() != null && telegramBot != null) {
            try {
                String notification = String.format(
                    "💬 Новое сообщение от %s %s (@%s)\n\n" +
                    "📝 %s\n\n" +
                    "💬 Ответить: /message_%d",
                    sender.getFirstName(),
                    sender.getLastName(),
                    sender.getUsername(),
                    content.length() > 100 ? content.substring(0, 100) + "..." : content,
                    sender.getId()
                );
                telegramBot.sendMessage(recipient.getTelegramId(), notification);
            } catch (Exception e) {
                log.error("Failed to send Telegram notification for private message", e);
            }
        }
        
        return saved;
    }
    
    public List<PrivateMessage> getConversation(User user1, User user2) {
        return messageRepository.findConversation(user1, user2);
    }
    
    public List<User> getConversationPartners(User user) {
        return messageRepository.findConversationPartners(user);
    }
    
    public List<PrivateMessage> getUnreadMessages(User user) {
        return messageRepository.findUnreadMessages(user);
    }
    
    public long getUnreadCount(User user) {
        try {
            return messageRepository.countByRecipientAndReadFalseAndDeletedByRecipientFalse(user);
        } catch (Exception e) {
            // Если таблица еще не создана, возвращаем 0
            return 0;
        }
    }
    
    public void markAsRead(Long messageId, User user) {
        PrivateMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("Сообщение не найдено"));
        
        if (message.getRecipient().getId().equals(user.getId()) && !message.getRead()) {
            message.setRead(true);
            message.setReadAt(LocalDateTime.now());
            messageRepository.save(message);
        }
    }
    
    public void markConversationAsRead(User currentUser, User otherUser) {
        List<PrivateMessage> unread = messageRepository.findUnreadMessages(currentUser);
        for (PrivateMessage message : unread) {
            if (message.getSender().getId().equals(otherUser.getId())) {
                message.setRead(true);
                message.setReadAt(LocalDateTime.now());
                messageRepository.save(message);
            }
        }
    }
    
    public void deleteMessage(Long messageId, User user) {
        PrivateMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("Сообщение не найдено"));
        
        if (message.getSender().getId().equals(user.getId())) {
            message.setDeletedBySender(true);
        } else if (message.getRecipient().getId().equals(user.getId())) {
            message.setDeletedByRecipient(true);
        }
        
        messageRepository.save(message);
    }
}

