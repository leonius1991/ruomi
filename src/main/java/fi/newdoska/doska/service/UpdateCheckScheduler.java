package fi.newdoska.doska.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateCheckScheduler {
    
    private final GitHubUpdateService githubUpdateService;
    private final VersionService versionService;
    
    private boolean hasNewVersion = false;
    private String latestVersion = null;
    
    @Scheduled(fixedRate = 3600000) // Каждый час (3600000 мс)
    public void checkForUpdates() {
        try {
            log.info("Автоматическая проверка обновлений...");
            GitHubUpdateService.ReleaseInfo latestRelease = githubUpdateService.getLatestRelease();
            
            if (latestRelease != null) {
                String currentVersion = versionService.getCurrentVersion();
                hasNewVersion = latestRelease.isNewer();
                latestVersion = latestRelease.getVersion();
                
                if (hasNewVersion) {
                    log.info("Обнаружена новая версия: {} (текущая: {})", latestVersion, currentVersion);
                } else {
                    log.debug("Установлена последняя версия: {}", currentVersion);
                }
            } else {
                log.warn("Не удалось получить информацию о последней версии");
            }
        } catch (Exception e) {
            log.error("Ошибка при автоматической проверке обновлений", e);
        }
    }
    
    public boolean hasNewVersion() {
        return hasNewVersion;
    }
    
    public String getLatestVersion() {
        return latestVersion;
    }
}

