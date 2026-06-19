package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.SearchLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    List<SearchLog> findTop10ByOrderByCreatedAtDesc();

    List<SearchLog> findTop50ByOrderByCreatedAtDesc();

    List<SearchLog> findTop10ByResultsCountOrderByCreatedAtDesc(Long resultsCount);

    long countByResultsCount(Long resultsCount);

    long countByCreatedAtAfter(java.time.LocalDateTime since);

    @Query("SELECT s.term, COUNT(s.id) FROM SearchLog s GROUP BY s.term ORDER BY COUNT(s.id) DESC")
    List<Object[]> findTopTerms(Pageable pageable);

    @Query("SELECT s.city, COUNT(s.id) FROM SearchLog s WHERE s.city IS NOT NULL AND s.city <> '' GROUP BY s.city ORDER BY COUNT(s.id) DESC")
    List<Object[]> findTopCities(Pageable pageable);

    @Query("SELECT COUNT(DISTINCT s.username) FROM SearchLog s WHERE s.username IS NOT NULL AND s.username <> ''")
    long countDistinctUsers();
}


