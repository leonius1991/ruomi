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

    List<SearchLog> findTop10ByResultsCountOrderByCreatedAtDesc(Long resultsCount);

    @Query("SELECT s.term AS term, COUNT(s.id) AS cnt FROM SearchLog s GROUP BY s.term ORDER BY cnt DESC")
    List<Object[]> findTopTerms(Pageable pageable);
}


