package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user-menu")
@RequiredArgsConstructor
public class UserMenuController {
    
    private final UserService userService;
    
    @GetMapping("/info/{userId}")
    public Map<String, Object> getUserInfo(@PathVariable Long userId, 
                                           @AuthenticationPrincipal User currentUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
            
            response.put("success", true);
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                "lastName", user.getLastName() != null ? user.getLastName() : "",
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "/images/default-avatar.png"
            ));
            response.put("canMessage", !user.getId().equals(currentUser.getId()));
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
    
    @PostMapping("/report")
    public Map<String, Object> reportUser(@RequestParam Long userId,
                                          @RequestParam String reason,
                                          @AuthenticationPrincipal User currentUser) {
        Map<String, Object> response = new HashMap<>();
        // TODO: Implement reporting logic
        response.put("success", true);
        response.put("message", "Жалоба отправлена");
        return response;
    }
}

