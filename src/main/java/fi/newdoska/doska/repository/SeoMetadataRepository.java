package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.SeoMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeoMetadataRepository extends JpaRepository<SeoMetadataEntity, Long> {
    Optional<SeoMetadataEntity> findByPageKey(String pageKey);
}


