package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.AppVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {
    List<AppVersion> findByIsCurrentTrue();
    
    @Query("SELECT v FROM AppVersion v WHERE v.isCurrent = true ORDER BY v.installedAt DESC")
    List<AppVersion> findCurrentVersionsOrdered();
    
    Optional<AppVersion> findByVersion(String version);
}

