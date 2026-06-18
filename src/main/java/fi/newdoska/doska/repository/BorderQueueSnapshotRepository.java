package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.BorderQueueSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BorderQueueSnapshotRepository extends JpaRepository<BorderQueueSnapshot, Long> {

    Optional<BorderQueueSnapshot> findFirstByCheckpointAndLaneOrderByCapturedAtDesc(String checkpoint, String lane);

    List<BorderQueueSnapshot> findByCapturedAtAfterOrderByCapturedAtAsc(LocalDateTime after);

    @Query("""
            SELECT s FROM BorderQueueSnapshot s
            WHERE s.capturedAt = (
                SELECT MAX(s2.capturedAt) FROM BorderQueueSnapshot s2
                WHERE s2.checkpoint = s.checkpoint AND s2.lane = s.lane
            )
            ORDER BY s.checkpoint, s.lane
            """)
    List<BorderQueueSnapshot> findLatestPerLane();
}
