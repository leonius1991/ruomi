package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.VisitStatistics;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface VisitStatisticsRepository extends JpaRepository<VisitStatistics, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VisitStatistics v WHERE v.id = 1")
    Optional<VisitStatistics> findForUpdate();
}
