package fi.newdoska.doska.config;

import fi.newdoska.doska.service.BorderQueueTrackerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BorderQueueStartupRunner implements ApplicationRunner {

    private final BorderQueueTrackerService trackerService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            trackerService.pollAndProcess();
            log.info("Первичный опрос очереди на границе выполнен");
        } catch (Exception e) {
            log.warn("Не удалось выполнить первичный опрос границы: {}", e.getMessage());
        }
    }
}
