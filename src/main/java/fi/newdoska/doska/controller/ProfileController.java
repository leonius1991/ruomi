package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.service.FileStorageService;
import fi.newdoska.doska.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {
    
    private final UserService userService;
    private final FileStorageService fileStorageService;
    
    @GetMapping
    public String profile(Model model) {
        String username = getCurrentUsername();
        if (username == null) {
            return "redirect:/login";
        }
        
        User user = (User) userService.loadUserByUsername(username);
        model.addAttribute("user", user);
        model.addAttribute("isOwnProfile", true);
        return "profile";
    }
    
    @GetMapping("/{userId}")
    public String viewProfile(@PathVariable Long userId, Model model) {
        User user = userService.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        model.addAttribute("user", user);
        model.addAttribute("isOwnProfile", false);
        return "profile";
    }
    
    @PostMapping("/upload-avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            String username = getCurrentUsername();
            User user = (User) userService.loadUserByUsername(username);
            
            if (!file.isEmpty()) {
                String fileName = fileStorageService.storeFile(file);
                user.setAvatarUrl(fileName);
                userService.saveUser(user);
                redirectAttributes.addFlashAttribute("success", "Аватар успешно загружен");
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при загрузке аватара: " + e.getMessage());
        }
        return "redirect:/profile";
    }
    
    @PostMapping("/update")
    public String updateProfile(@RequestParam(required = false) String firstName,
                               @RequestParam(required = false) String lastName,
                               @RequestParam(required = false) String phone,
                               RedirectAttributes redirectAttributes) {
        try {
            String username = getCurrentUsername();
            User user = (User) userService.loadUserByUsername(username);
            
            if (firstName != null && !firstName.trim().isEmpty()) {
                user.setFirstName(firstName.trim());
            }
            if (lastName != null && !lastName.trim().isEmpty()) {
                user.setLastName(lastName.trim());
            }
            if (phone != null) {
                user.setPhone(phone.trim().isEmpty() ? null : phone.trim());
            }
            
            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("success", "Профиль успешно обновлен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении профиля: " + e.getMessage());
        }
        return "redirect:/profile";
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getUsername();
        }
        return authentication.getName();
    }
}


