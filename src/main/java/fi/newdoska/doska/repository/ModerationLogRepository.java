package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.ModerationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModerationLogRepository extends JpaRepository<ModerationLog, Long> {
    Page<ModerationLog> findByAdvertisementIdOrderByCreatedAtDesc(Long advertisementId, Pageable pageable);
    List<ModerationLog> findByModeratorIdOrderByCreatedAtDesc(Long moderatorId);
    Page<ModerationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}


