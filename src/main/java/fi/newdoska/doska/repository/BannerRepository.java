package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByPositionAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String position, LocalDateTime now1, LocalDateTime now2);
    List<Banner> findByActiveTrue();
}

