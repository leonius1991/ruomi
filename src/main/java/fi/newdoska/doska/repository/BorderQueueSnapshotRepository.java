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

    @Query(value = """
            SELECT checkpoint, lane, MAX(live_count) AS max_queue
            FROM border_queue_snapshots
            GROUP BY checkpoint, lane
            ORDER BY max_queue DESC
            """, nativeQuery = true)
    List<Object[]> findMaxLiveCountByCheckpointAndLane();

    @Query(value = """
            SELECT checkpoint, lane, HOUR(captured_at) AS hr, AVG(live_count) AS avg_queue
            FROM border_queue_snapshots
            GROUP BY checkpoint, lane, HOUR(captured_at)
            ORDER BY avg_queue ASC
            """, nativeQuery = true)
    List<Object[]> findAvgLiveCountByHour();

    @Query(value = """
            SELECT checkpoint, lane, AVG(live_count) AS avg_queue
            FROM border_queue_snapshots
            GROUP BY checkpoint, lane
            """, nativeQuery = true)
    List<Object[]> findOverallAvgLiveCount();

    @Query(value = "SELECT COUNT(*) FROM border_queue_snapshots", nativeQuery = true)
    long countSnapshots();

    @Query(value = "SELECT MIN(captured_at) FROM border_queue_snapshots", nativeQuery = true)
    LocalDateTime findEarliestSnapshotTime();
}
