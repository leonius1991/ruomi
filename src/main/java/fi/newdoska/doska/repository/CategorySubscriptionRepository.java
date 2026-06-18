package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.Category;
import fi.newdoska.doska.entity.CategorySubscription;
import fi.newdoska.doska.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategorySubscriptionRepository extends JpaRepository<CategorySubscription, Long> {
    
    List<CategorySubscription> findByUserAndActiveTrue(User user);
    
    List<CategorySubscription> findByCategoryAndActiveTrue(Category category);
    
    Optional<CategorySubscription> findByUserAndCategory(User user, Category category);
    
    long countByCategoryAndActiveTrue(Category category);
    
    long countByActiveTrue();

    void deleteByCategory(Category category);
}

