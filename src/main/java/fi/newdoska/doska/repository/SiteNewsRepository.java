package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.SiteNews;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteNewsRepository extends JpaRepository<SiteNews, Long> {
    List<SiteNews> findByPublishedTrueOrderByCreatedAtDesc();
    List<SiteNews> findAllByOrderByCreatedAtDesc();
}
