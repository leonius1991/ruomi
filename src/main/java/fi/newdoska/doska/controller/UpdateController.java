package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.AppVersion;
import fi.newdoska.doska.service.GitHubUpdateService;
import fi.newdoska.doska.service.UpdateService;
import fi.newdoska.doska.service.VersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/updates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Slf4j
public class UpdateController {
    
    private final VersionService versionService;
    private final GitHubUpdateService githubUpdateService;
    private final UpdateService updateService;
    
    @GetMapping
    public String updatesPage(Model model) {
        AppVersion currentVersion = versionService.getCurrentVersionEntity();
        GitHubUpdateService.ReleaseInfo latestRelease = githubUpdateService.getLatestRelease();
        
        model.addAttribute("currentVersion", currentVersion);
        model.addAttribute("latestRelease", latestRelease);
        model.addAttribute("hasNewVersion", latestRelease != null && latestRelease.isNewer());
        model.addAttribute("updateStatus", updateService.getCurrentStatus());
        
        return "admin/updates";
    }
    
    @GetMapping("/api/check")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkUpdates() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            GitHubUpdateService.ReleaseInfo latestRelease = githubUpdateService.getLatestRelease();
            String currentVersion = versionService.getCurrentVersion();
            
            response.put("success", true);
            response.put("currentVersion", currentVersion);
            response.put("latestRelease", latestRelease);
            response.put("hasNewVersion", latestRelease != null && latestRelease.isNewer());
            
            if (latestRelease != null) {
                response.put("newVersion", latestRelease.getVersion());
                response.put("releaseNotes", latestRelease.getReleaseNotes());
                response.put("publishedAt", latestRelease.getPublishedAt());
            }
            } catch (Exception e) {
                log.error("Ошибка при проверке обновлений", e);
                response.put("success", false);
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("403")) {
                    errorMessage = "Ошибка доступа к GitHub (403 Forbidden). " +
                            "Проверьте настройки GitHub token в application.properties. " +
                            "Для приватных репозиториев требуется токен с правами на чтение.";
                }
                response.put("error", errorMessage);
            }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/api/status")
    @ResponseBody
    public ResponseEntity<UpdateService.UpdateProgress> getUpdateStatus() {
        return ResponseEntity.ok(updateService.getUpdateProgress());
    }
    
    @PostMapping("/api/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startUpdate(@RequestParam(required = false) String version) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Проверяем, не идет ли уже обновление
            if (updateService.getCurrentStatus() != UpdateService.UpdateStatus.IDLE &&
                updateService.getCurrentStatus() != UpdateService.UpdateStatus.SUCCESS &&
                updateService.getCurrentStatus() != UpdateService.UpdateStatus.FAILED) {
                response.put("success", false);
                response.put("error", "Обновление уже выполняется");
                return ResponseEntity.ok(response);
            }
            
            GitHubUpdateService.ReleaseInfo latestRelease = githubUpdateService.getLatestRelease();
            
            if (latestRelease == null) {
                response.put("success", false);
                response.put("error", "Не удалось получить информацию о последнем релизе");
                return ResponseEntity.ok(response);
            }
            
            if (latestRelease.getDownloadUrl() == null || latestRelease.getDownloadUrl().isEmpty()) {
                response.put("success", false);
                response.put("error", "URL для скачивания не найден в релизе");
                return ResponseEntity.ok(response);
            }
            
            String targetVersion = version != null ? version : latestRelease.getVersion();
            String resourcesUrl = latestRelease.getResourcesUrl();
            
            // Запускаем обновление асинхронно
            updateService.performUpdate(latestRelease.getDownloadUrl(), targetVersion, resourcesUrl)
                    .thenAccept(success -> {
                        if (success) {
                            log.info("Обновление завершено успешно");
                        } else {
                            log.error("Обновление завершилось с ошибкой");
                        }
                    });
            
            response.put("success", true);
            response.put("message", "Обновление запущено");
            response.put("version", targetVersion);
            
        } catch (Exception e) {
            log.error("Ошибка при запуске обновления", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/api/releases")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllReleases() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<GitHubUpdateService.ReleaseInfo> releases = githubUpdateService.getAllReleases();
            response.put("success", true);
            response.put("releases", releases);
        } catch (Exception e) {
            log.error("Ошибка при получении списка релизов", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}

