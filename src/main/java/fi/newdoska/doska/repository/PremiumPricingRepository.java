package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.PremiumPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PremiumPricingRepository extends JpaRepository<PremiumPricing, Long> {
}


