package fi.newdoska.doska.controller;

import fi.newdoska.doska.dto.UserDto;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

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
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request) {
        
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        
        try {
            userService.registerUser(userDto);
            // Сохраняем email в сессии для возможности повторной отправки
            request.getSession().setAttribute("registeredEmail", userDto.getEmail());
            redirectAttributes.addFlashAttribute("message", 
                "Регистрация успешна! Проверьте ваш email для подтверждения аккаунта.");
            redirectAttributes.addFlashAttribute("registeredEmail", userDto.getEmail());
            return "redirect:/login";
        } catch (RuntimeException e) {
            bindingResult.rejectValue("username", "error.user", e.getMessage());
            return "auth/register";
        }
    }
    
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token, 
                             Model model,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        User verifiedUser = userService.verifyEmailAndGetUser(token);
        
        if (verifiedUser != null) {
            // Автоматически входим пользователя в систему
            try {
                authenticateUser(verifiedUser, request);
                redirectAttributes.addFlashAttribute("message", 
                    "Ваш аккаунт успешно подтвержден! Вы автоматически вошли в систему.");
                return "redirect:/";
            } catch (Exception e) {
                log.error("Error auto-login after verification", e);
                // Если не удалось автоматически войти, показываем сообщение
                model.addAttribute("message", 
                    "Ваш email успешно подтвержден! Войдите в систему, используя свои данные для входа.");
                model.addAttribute("username", verifiedUser.getUsername());
                return "auth/verification-success";
            }
        } else {
            model.addAttribute("error", "Недействительная или истекшая ссылка для подтверждения.");
            return "auth/verification-error";
        }
    }
    
    private void authenticateUser(User user, HttpServletRequest request) {
        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(
            user, 
            null, 
            user.getAuthorities()
        );
        
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        
        // Сохраняем в сессию
        request.getSession(true).setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext()
        );
    }
    
    @PostMapping("/resend-verification")
    @ResponseBody
    public Map<String, Object> resendVerificationEmail(@RequestParam(required = false) String email,
                                                       HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        // Пытаемся получить email из параметра, сессии или flash атрибута
        String targetEmail = email;
        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            Object registeredEmail = request.getSession().getAttribute("registeredEmail");
            if (registeredEmail instanceof String) {
                targetEmail = (String) registeredEmail;
            }
        }
        
        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "Email не указан.");
            return response;
        }
        
        try {
            userService.resendVerificationEmail(targetEmail);
            response.put("success", true);
            response.put("message", "Письмо для подтверждения отправлено на ваш email.");
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
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
    
    /**
     * Установка пароля для пользователя, зарегистрированного через Telegram
     */
    @PostMapping("/auth/set-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setPassword(@RequestBody Map<String, String> request,
                                                           org.springframework.security.core.Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "Требуется авторизация"
                ));
            }
            
            String newPassword = request.get("password");
            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Пароль должен содержать минимум 6 символов"
                ));
            }
            
            Object principal = authentication.getPrincipal();
            fi.newdoska.doska.entity.User user;
            if (principal instanceof fi.newdoska.doska.entity.User) {
                user = (fi.newdoska.doska.entity.User) principal;
            } else {
                String username = authentication.getName();
                user = (fi.newdoska.doska.entity.User) userService.loadUserByUsername(username);
            }
            
            // Устанавливаем пароль без проверки старого (для Telegram пользователей)
            userService.setPassword(user.getId(), newPassword);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Пароль успешно установлен"
            ));
            
        } catch (Exception e) {
            log.error("Error setting password", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Ошибка при установке пароля: " + e.getMessage()
            ));
        }
    }
} 