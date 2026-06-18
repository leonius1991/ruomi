package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.entity.Favorite;
import fi.newdoska.doska.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    Optional<Favorite> findByUserAndAdvertisement(User user, Advertisement advertisement);
    
    Page<Favorite> findByUserOrderByAddedAtDesc(User user, Pageable pageable);
    
    List<Favorite> findByUserOrderByAddedAtDesc(User user);
    
    boolean existsByUserAndAdvertisement(User user, Advertisement advertisement);
    
    void deleteByUserAndAdvertisement(User user, Advertisement advertisement);
}
