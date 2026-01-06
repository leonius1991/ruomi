package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.Advertisement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
    
    Page<Advertisement> findByStatus(Advertisement.Status status, Pageable pageable);
    
    Page<Advertisement> findByCategory(Advertisement.Category category, Pageable pageable);
    
    Page<Advertisement> findByUserId(Long userId, Pageable pageable);
    
    Page<Advertisement> findByCity(String city, Pageable pageable);
    
    @Query("SELECT a FROM Advertisement a WHERE a.status = 'APPROVED' AND a.publishedAt IS NOT NULL ORDER BY a.isPremium DESC, a.isUrgent DESC, a.publishedAt DESC")
    Page<Advertisement> findApprovedAdvertisements(Pageable pageable);
    
    @Query("SELECT a FROM Advertisement a WHERE a.status = 'APPROVED' AND a.publishedAt IS NOT NULL AND a.category = :category ORDER BY a.isPremium DESC, a.isUrgent DESC, a.publishedAt DESC")
    Page<Advertisement> findApprovedAdvertisementsByCategory(@Param("category") Advertisement.Category category, Pageable pageable);
    
    @Query("SELECT a FROM Advertisement a WHERE a.status = 'APPROVED' AND a.publishedAt IS NOT NULL AND (a.title LIKE %:searchTerm% OR a.description LIKE %:searchTerm%) ORDER BY a.isPremium DESC, a.isUrgent DESC, a.publishedAt DESC")
    Page<Advertisement> searchApprovedAdvertisements(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT a FROM Advertisement a WHERE a.status = 'APPROVED' AND a.publishedAt IS NOT NULL AND a.price BETWEEN :minPrice AND :maxPrice ORDER BY a.isPremium DESC, a.isUrgent DESC, a.publishedAt DESC")
    Page<Advertisement> findAdvertisementsByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice, Pageable pageable);
    
    @Query("SELECT a FROM Advertisement a WHERE a.status = 'APPROVED' AND a.publishedAt IS NOT NULL AND a.city = :city AND a.category = :category ORDER BY a.isPremium DESC, a.isUrgent DESC, a.publishedAt DESC")
    Page<Advertisement> findAdvertisementsByCityAndCategory(@Param("city") String city, @Param("category") Advertisement.Category category, Pageable pageable);
    
    @Query("SELECT a FROM Advertisement a WHERE a.status = :status AND a.publishedAt IS NOT NULL AND a.category = :category AND a.type = :type ORDER BY a.isPremium DESC, a.isUrgent DESC, a.publishedAt DESC")
    Page<Advertisement> findByStatusAndCategoryAndType(@Param("status") Advertisement.Status status, @Param("category") Advertisement.Category category, @Param("type") Advertisement.AdvertisementType type, Pageable pageable);
    
    @Query("SELECT a FROM Advertisement a WHERE a.status = 'APPROVED' AND a.publishedAt IS NOT NULL AND a.isPremium = true ORDER BY a.publishedAt DESC")
    List<Advertisement> findPremiumAdvertisements();
    
    @Query("SELECT a FROM Advertisement a WHERE a.status = 'APPROVED' AND a.publishedAt IS NOT NULL AND a.isUrgent = true ORDER BY a.publishedAt DESC")
    List<Advertisement> findUrgentAdvertisements();
    
    @Query("SELECT a FROM Advertisement a WHERE a.status = 'APPROVED' AND a.publishedAt IS NOT NULL AND a.expiresAt <= :expiryDate")
    List<Advertisement> findExpiringAdvertisements(@Param("expiryDate") LocalDateTime expiryDate);
    
    @Query("SELECT COUNT(a) FROM Advertisement a WHERE a.status = 'APPROVED' AND a.publishedAt IS NOT NULL")
    Long countApprovedAdvertisements();
    
    @Query("SELECT COUNT(a) FROM Advertisement a WHERE a.status = 'PENDING'")
    Long countPendingAdvertisements();
    
    List<Advertisement> findByUserId(Long userId);
} 