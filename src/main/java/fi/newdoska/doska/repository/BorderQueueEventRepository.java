package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.BorderQueueEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BorderQueueEventRepository extends JpaRepository<BorderQueueEvent, Long> {

    List<BorderQueueEvent> findTop50ByOrderByRecordedAtDesc();

    List<BorderQueueEvent> findByRecordedAtAfterOrderByRecordedAtAsc(LocalDateTime after);

    @Query(value = """
            SELECT checkpoint, lane, DATE(recorded_at) AS day, SUM(delta) AS total
            FROM border_queue_events
            WHERE event_type = 'EXIT' AND recorded_at >= :from
            GROUP BY checkpoint, lane, DATE(recorded_at)
            ORDER BY day ASC
            """, nativeQuery = true)
    List<Object[]> sumDailyExitsByLaneSince(@Param("from") LocalDateTime from);
}
