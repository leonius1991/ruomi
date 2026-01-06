package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class UserManagementController {

    private final UserService userService;

    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Model model) {
        
        Page<User> users;
        if (search != null && !search.trim().isEmpty()) {
            users = userService.searchUsers(search.trim(), PageRequest.of(page, size));
        } else {
            users = userService.getAllUsers(PageRequest.of(page, size));
        }
        
        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("roles", User.UserRole.values());
        return "admin/users";
    }

    @PostMapping("/{id}/role")
    public String updateUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserRole(id, User.UserRole.valueOf(role));
            redirectAttributes.addFlashAttribute("success", "Роль пользователя обновлена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/premium")
    public String togglePremiumStatus(
            @PathVariable Long id,
            @RequestParam boolean isPremium,
            RedirectAttributes redirectAttributes) {
        try {
            userService.setPremiumStatus(id, isPremium);
            redirectAttributes.addFlashAttribute("success", 
                isPremium ? "Премиум статус активирован" : "Премиум статус деактивирован");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/enable")
    public String toggleUserEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled,
            RedirectAttributes redirectAttributes) {
        try {
            userService.setUserEnabled(id, enabled);
            redirectAttributes.addFlashAttribute("success", 
                enabled ? "Пользователь активирован" : "Пользователь деактивирован");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Пользователь удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}


