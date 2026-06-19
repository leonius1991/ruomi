package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.AppSetting;
import fi.newdoska.doska.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppSettingsService {

    public static final String DOSKA_IMPORT_KEY = "doska.import.enabled";

    private final AppSettingRepository repository;

    @Value("${doska.import.enabled:true}")
    private boolean doskaImportDefault;

    public boolean isDoskaImportEnabled() {
        return repository.findById(DOSKA_IMPORT_KEY)
                .map(s -> "true".equalsIgnoreCase(s.getValue()))
                .orElse(doskaImportDefault);
    }

    @Transactional
    public void setDoskaImportEnabled(boolean enabled) {
        AppSetting setting = repository.findById(DOSKA_IMPORT_KEY).orElse(new AppSetting());
        setting.setKey(DOSKA_IMPORT_KEY);
        setting.setValue(enabled ? "true" : "false");
        setting.setUpdatedAt(LocalDateTime.now());
        repository.save(setting);
    }
}
