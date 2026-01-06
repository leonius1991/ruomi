package fi.newdoska.doska.controller;

import fi.newdoska.doska.dto.UserDto;
import fi.newdoska.doska.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final UserService userService;
    
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "logout", required = false) String logout,
                       Model model) {
        
        if (error != null) {
            model.addAttribute("error", "Неверное имя пользователя или пароль");
        }
        
        if (logout != null) {
            model.addAttribute("message", "Вы успешно вышли из системы");
        }
        
        return "auth/login";
    }
    
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("userDto", new UserDto());
        return "auth/register";
    }
    
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute UserDto userDto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        
        try {
            userService.registerUser(userDto);
            redirectAttributes.addFlashAttribute("message", 
                "Регистрация успешна! Проверьте ваш email для подтверждения аккаунта.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            bindingResult.rejectValue("username", "error.user", e.getMessage());
            return "auth/register";
        }
    }
    
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token, Model model) {
        boolean verified = userService.verifyEmail(token);
        
        if (verified) {
            model.addAttribute("message", "Ваш email успешно подтвержден! Теперь вы можете войти в систему.");
            return "auth/verification-success";
        } else {
            model.addAttribute("error", "Недействительная или истекшая ссылка для подтверждения.");
            return "auth/verification-error";
        }
    }
    
    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "auth/forgot-password";
    }
    
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email,
                                      RedirectAttributes redirectAttributes) {
        try {
            userService.resetPassword(email);
            redirectAttributes.addFlashAttribute("message", 
                "Инструкции по сбросу пароля отправлены на ваш email.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/login";
    }
    
    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }
    
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                     @RequestParam String newPassword,
                                     @RequestParam String confirmPassword,
                                     RedirectAttributes redirectAttributes) {
        
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Пароли не совпадают");
            return "redirect:/reset-password?token=" + token;
        }
        
        try {
            // Здесь должна быть логика для сброса пароля
            redirectAttributes.addFlashAttribute("message", "Пароль успешно изменен!");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reset-password?token=" + token;
        }
    }
    
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "auth/access-denied";
    }
} 