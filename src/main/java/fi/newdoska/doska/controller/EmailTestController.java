package fi.newdoska.doska.controller;

import fi.newdoska.doska.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/email-test")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Slf4j
public class EmailTestController {

    private final EmailService emailService;

    @GetMapping
    public String emailTestPage(Model model) {
        // Список доступных шаблонов
        List<Map<String, String>> templates = Arrays.asList(
            createTemplateInfo("verification", "Подтверждение регистрации", "Письмо с ссылкой для подтверждения email"),
            createTemplateInfo("welcome", "Приветственное письмо", "Письмо после успешной регистрации"),
            createTemplateInfo("password_reset", "Сброс пароля", "Письмо со ссылкой для сброса пароля"),
            createTemplateInfo("ad_approved", "Одобрение объявления", "Уведомление об одобрении объявления"),
            createTemplateInfo("ad_rejected", "Отклонение объявления", "Уведомление об отклонении объявления"),
            createTemplateInfo("telegram_password", "Пароль для Telegram", "Письмо с паролем после регистрации через Telegram"),
            createTemplateInfo("private_message", "Личное сообщение", "Уведомление о новом личном сообщении"),
            createTemplateInfo("search_alert", "Уведомление о поиске", "Уведомление о популярном поиске без результатов")
        );
        
        model.addAttribute("templates", templates);
        return "admin/email-test";
    }

    @PostMapping("/send")
    @ResponseBody
    public Map<String, Object> sendTestEmail(
            @RequestParam String email,
            @RequestParam String templateType,
            RedirectAttributes redirectAttributes) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Валидация email
            if (email == null || email.trim().isEmpty() || !email.contains("@")) {
                response.put("success", false);
                response.put("error", "Неверный формат email адреса");
                return response;
            }
            
            // Отправка тестового письма
            boolean sent = emailService.sendTestEmail(email.trim(), templateType);
            
            if (sent) {
                response.put("success", true);
                response.put("message", "Тестовое письмо успешно отправлено на " + email);
                log.info("Test email sent successfully to: {} with template: {}", email, templateType);
            } else {
                response.put("success", false);
                response.put("error", "Не удалось отправить письмо. Проверьте логи.");
            }
            
        } catch (Exception e) {
            log.error("Error sending test email", e);
            response.put("success", false);
            response.put("error", "Ошибка при отправке: " + e.getMessage());
        }
        
        return response;
    }

    private Map<String, String> createTemplateInfo(String value, String name, String description) {
        Map<String, String> info = new HashMap<>();
        info.put("value", value);
        info.put("name", name);
        info.put("description", description);
        return info;
    }
}


