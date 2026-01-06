package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.AdvertisementTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvertisementTypeRepository extends JpaRepository<AdvertisementTypeEntity, Long> {
    List<AdvertisementTypeEntity> findByCategoryIdAndActiveTrueOrderBySortOrderAsc(Long categoryId);
    List<AdvertisementTypeEntity> findByCategoryIsNullAndActiveTrueOrderBySortOrderAsc();
    List<AdvertisementTypeEntity> findByActiveTrueOrderBySortOrderAsc();
}


