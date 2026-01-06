package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.AppVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {
    Optional<AppVersion> findByIsCurrentTrue();
    Optional<AppVersion> findByVersion(String version);
}

