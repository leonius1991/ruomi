package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.AppVersion;
import fi.newdoska.doska.repository.AppVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VersionService {
    
    private final AppVersionRepository appVersionRepository;
    
    @Value("${app.version:0.0.1-SNAPSHOT}")
    private String currentVersion;
    
    @PostConstruct
    @Transactional
    public void initializeVersion() {
        try {
            // Пытаемся получить версию из pom.xml через properties
            Optional<AppVersion> existingVersion = appVersionRepository.findByVersion(currentVersion);
            
            if (existingVersion.isEmpty()) {
                // Снимаем флаг текущей версии со всех
                appVersionRepository.findAll().forEach(v -> v.setIsCurrent(false));
                appVersionRepository.saveAll(appVersionRepository.findAll());
                
                // Создаем новую запись о версии
                AppVersion version = new AppVersion();
                version.setVersion(currentVersion);
                version.setIsCurrent(true);
                version.setInstalledAt(LocalDateTime.now());
                version.setUpdateStatus("SUCCESS");
                appVersionRepository.save(version);
                
                log.info("Инициализирована версия приложения: {}", currentVersion);
            } else {
                // Обновляем флаг текущей версии
                appVersionRepository.findAll().forEach(v -> v.setIsCurrent(false));
                existingVersion.get().setIsCurrent(true);
                appVersionRepository.save(existingVersion.get());
            }
        } catch (Exception e) {
            log.error("Ошибка при инициализации версии", e);
        }
    }
    
    public String getCurrentVersion() {
        return appVersionRepository.findByIsCurrentTrue()
                .map(AppVersion::getVersion)
                .orElse(currentVersion);
    }
    
    public AppVersion getCurrentVersionEntity() {
        return appVersionRepository.findByIsCurrentTrue()
                .orElseGet(() -> {
                    AppVersion version = new AppVersion();
                    version.setVersion(currentVersion);
                    version.setIsCurrent(true);
                    return version;
                });
    }
}

