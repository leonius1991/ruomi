package fi.newdoska.doska.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BorderQueueScheduler {

    private final BorderQueueTrackerService trackerService;

    @Scheduled(fixedRateString = "${border.queue.poll-interval-ms:180000}")
    public void pollBorderQueues() {
        try {
            trackerService.pollAndProcess();
        } catch (Exception e) {
            log.error("Ошибка опроса очереди на границе", e);
        }
    }
}
