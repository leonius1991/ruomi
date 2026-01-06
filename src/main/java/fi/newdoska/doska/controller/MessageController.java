package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.PrivateMessage;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.service.PrivateMessageService;
import fi.newdoska.doska.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {
    
    private final PrivateMessageService messageService;
    private final UserService userService;
    
    @GetMapping
    public String showMessages(@AuthenticationPrincipal User user, Model model) {
        List<User> partners = messageService.getConversationPartners(user);
        long unreadCount = messageService.getUnreadCount(user);
        
        model.addAttribute("partners", partners);
        model.addAttribute("unreadCount", unreadCount);
        return "messages";
    }
    
    @GetMapping("/conversation/{userId}")
    public String showConversation(@AuthenticationPrincipal User currentUser, 
                                   @PathVariable Long userId, Model model) {
        User otherUser = userService.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        List<PrivateMessage> messages = messageService.getConversation(currentUser, otherUser);
        messageService.markConversationAsRead(currentUser, otherUser);
        
        model.addAttribute("messages", messages);
        model.addAttribute("otherUser", otherUser);
        return "messages/conversation";
    }
    
    @PostMapping("/send")
    @ResponseBody
    public String sendMessage(@AuthenticationPrincipal User sender,
                             @RequestParam Long recipientId,
                             @RequestParam String content) {
        try {
            User recipient = userService.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
            messageService.sendMessage(sender, recipient, content);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    
    @PostMapping("/mark-read/{messageId}")
    @ResponseBody
    public String markAsRead(@AuthenticationPrincipal User user, @PathVariable Long messageId) {
        try {
            messageService.markAsRead(messageId, user);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    
    @PostMapping("/delete/{messageId}")
    @ResponseBody
    public String deleteMessage(@AuthenticationPrincipal User user, @PathVariable Long messageId) {
        try {
            messageService.deleteMessage(messageId, user);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    
    @GetMapping("/api/unread-count")
    @ResponseBody
    public Map<String, Object> getUnreadCount(@AuthenticationPrincipal User user) {
        Map<String, Object> response = new HashMap<>();
        if (user != null) {
            response.put("count", messageService.getUnreadCount(user));
        } else {
            response.put("count", 0);
        }
        return response;
    }
}

