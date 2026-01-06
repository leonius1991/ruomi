package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.entity.ModerationLog;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.ModerationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ModerationLogService {
    
    private final ModerationLogRepository moderationLogRepository;
    
    public ModerationLog logAction(Advertisement advertisement, User moderator, ModerationLog.Action action, String comment) {
        ModerationLog log = new ModerationLog();
        log.setAdvertisement(advertisement);
        log.setModerator(moderator);
        log.setAction(action);
        log.setComment(comment);
        return moderationLogRepository.save(log);
    }
    
    public Page<ModerationLog> getLogsByAdvertisement(Long advertisementId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return moderationLogRepository.findByAdvertisementIdOrderByCreatedAtDesc(advertisementId, pageable);
    }
    
    public List<ModerationLog> getLogsByModerator(Long moderatorId) {
        return moderationLogRepository.findByModeratorIdOrderByCreatedAtDesc(moderatorId);
    }
    
    public Page<ModerationLog> getAllLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return moderationLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}


