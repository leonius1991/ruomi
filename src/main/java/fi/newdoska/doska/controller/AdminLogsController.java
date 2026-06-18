package fi.newdoska.doska.controller;

import fi.newdoska.doska.service.UpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Slf4j
public class AdminLogsController {
    
    private final UpdateService updateService;
    
    @Value("${LOG_PATH:./logs}")
    private String logPath;
    
    @Value("${LOG_FILE:ruomi}")
    private String logFile;
    
    @GetMapping
    public String viewLogs(@RequestParam(defaultValue = "100") int lines, Model model) {
        model.addAttribute("lines", lines);
        
        // Читаем логи из файла
        List<String> logEntries = readLogFile(lines);
        model.addAttribute("logEntries", logEntries);
        
        // Читаем логи обновлений
        List<String> updateLogs = updateService.getUpdateLogs();
        model.addAttribute("updateLogs", updateLogs);
        
        return "admin/logs";
    }
    
    @PostMapping("/restart")
    @ResponseBody
    public java.util.Map<String, Object> restartApplication() {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            // Запускаем перезапуск в отдельном потоке, чтобы не блокировать ответ
            new Thread(() -> {
                try {
                    updateService.restartApplication();
                } catch (Exception e) {
                    log.error("Error restarting application", e);
                }
            }).start();
            response.put("success", true);
            response.put("message", "Команда перезапуска отправлена. Приложение будет перезапущено через несколько секунд.");
        } catch (Exception e) {
            log.error("Error initiating restart", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
    
    @GetMapping("/api/latest")
    @ResponseBody
    public java.util.Map<String, Object> getLatestLogs(@RequestParam(defaultValue = "100") int lines) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        List<String> logEntries = readLogFile(lines);
        response.put("logs", logEntries);
        response.put("count", logEntries.size());
        return response;
    }
    
    private List<String> readLogFile(int maxLines) {
        List<String> logEntries = new ArrayList<>();
        try {
            Path logFilePath = Paths.get(logPath, logFile + ".log");
            if (!Files.exists(logFilePath)) {
                // Пробуем альтернативные пути
                logFilePath = Paths.get("./logs", logFile + ".log");
                if (!Files.exists(logFilePath)) {
                    logFilePath = Paths.get("/opt/ruomi/logs", logFile + ".log");
                }
            }
            
            if (Files.exists(logFilePath)) {
                try (Stream<String> lines = Files.lines(logFilePath)) {
                    logEntries = lines
                        .skip(Math.max(0, Files.lines(logFilePath).count() - maxLines))
                        .collect(Collectors.toList());
                }
            } else {
                logEntries.add("Лог файл не найден: " + logFilePath);
            }
        } catch (Exception e) {
            log.error("Error reading log file", e);
            logEntries.add("Ошибка при чтении лог файла: " + e.getMessage());
        }
        return logEntries;
    }
}
