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
import java.util.List;
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
    
    @Transactional
    public String getCurrentVersion() {
        List<AppVersion> currentVersions = appVersionRepository.findCurrentVersionsOrdered();
        
        if (currentVersions.isEmpty()) {
            return currentVersion;
        }
        
        // Если несколько текущих версий, исправляем это
        if (currentVersions.size() > 1) {
            log.warn("Найдено {} текущих версий, исправляем...", currentVersions.size());
            // Оставляем только самую новую как текущую
            AppVersion latest = currentVersions.get(0);
            for (int i = 1; i < currentVersions.size(); i++) {
                currentVersions.get(i).setIsCurrent(false);
                appVersionRepository.save(currentVersions.get(i));
            }
            latest.setIsCurrent(true);
            appVersionRepository.save(latest);
            return latest.getVersion();
        }
        
        return currentVersions.get(0).getVersion();
    }
    
    @Transactional
    public AppVersion getCurrentVersionEntity() {
        List<AppVersion> currentVersions = appVersionRepository.findCurrentVersionsOrdered();
        
        if (currentVersions.isEmpty()) {
            // Создаем новую запись, если нет текущей версии
            AppVersion version = new AppVersion();
            version.setVersion(currentVersion);
            version.setIsCurrent(true);
            version.setInstalledAt(LocalDateTime.now());
            version.setUpdateStatus("SUCCESS");
            return appVersionRepository.save(version);
        }
        
        // Если несколько текущих версий, исправляем это
        if (currentVersions.size() > 1) {
            log.warn("Найдено {} текущих версий, исправляем...", currentVersions.size());
            // Оставляем только самую новую как текущую
            AppVersion latest = currentVersions.get(0);
            for (int i = 1; i < currentVersions.size(); i++) {
                currentVersions.get(i).setIsCurrent(false);
                appVersionRepository.save(currentVersions.get(i));
            }
            latest.setIsCurrent(true);
            return appVersionRepository.save(latest);
        }
        
        return currentVersions.get(0);
    }
}

