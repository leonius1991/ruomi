package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.AdvertisementImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvertisementImageRepository extends JpaRepository<AdvertisementImage, Long> {
    List<AdvertisementImage> findByAdvertisementId(Long advertisementId);
    void deleteByAdvertisementId(Long advertisementId);
}


