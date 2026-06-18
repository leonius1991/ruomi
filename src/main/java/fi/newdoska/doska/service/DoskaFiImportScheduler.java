package fi.newdoska.doska.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoskaFiImportScheduler {

    private final DoskaFiImportService importService;

    @Value("${doska.import.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${doska.import.cron:0 0 4 * * *}")
    public void runDailyImport() {
        if (!enabled) {
            return;
        }
        log.info("Запуск ежедневного импорта объявлений с doska.fi");
        try {
            DoskaFiImportService.ImportResult result = importService.importLatest();
            log.info("Ежедневный импорт doska.fi: +{} новых, {} уже были, {} ошибок",
                    result.imported(), result.skipped(), result.failed());
        } catch (Exception e) {
            log.error("Ошибка ежедневного импорта doska.fi", e);
        }
    }
}
