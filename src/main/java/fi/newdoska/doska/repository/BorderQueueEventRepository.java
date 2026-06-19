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

    @Query(value = """
            SELECT checkpoint, SUM(delta) FROM border_queue_events
            WHERE event_type = 'EXIT' GROUP BY checkpoint
            """, nativeQuery = true)
    List<Object[]> sumTotalExitsByCheckpoint();

    @Query(value = """
            SELECT HOUR(recorded_at) AS hr, checkpoint, SUM(delta) AS total
            FROM border_queue_events
            WHERE event_type = 'EXIT' AND lane = :lane
            GROUP BY HOUR(recorded_at), checkpoint
            ORDER BY total DESC
            """, nativeQuery = true)
    List<Object[]> sumExitsByHourAndCheckpoint(@Param("lane") String lane);

    @Query(value = """
            SELECT DAYOFWEEK(recorded_at) AS dow, checkpoint, SUM(delta) AS total
            FROM border_queue_events
            WHERE event_type = 'EXIT' AND lane = :lane
            GROUP BY DAYOFWEEK(recorded_at), checkpoint
            ORDER BY total DESC
            """, nativeQuery = true)
    List<Object[]> sumExitsByDayOfWeekAndCheckpoint(@Param("lane") String lane);

    @Query(value = """
            SELECT COUNT(*) FROM border_queue_events WHERE event_type = 'EXIT'
            """, nativeQuery = true)
    long countExitEvents();

    @Query(value = "SELECT MIN(recorded_at) FROM border_queue_events", nativeQuery = true)
    LocalDateTime findEarliestEventTime();
}
