package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.SiteTheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteThemeRepository extends JpaRepository<SiteTheme, Long> {
}


