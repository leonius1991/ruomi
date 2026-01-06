package fi.newdoska.doska.repository;

import fi.newdoska.doska.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByTransactionId(String transactionId);
    
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Payment> findByAdvertisementIdOrderByCreatedAtDesc(Long advertisementId);
    
    List<Payment> findByStatus(Payment.PaymentStatus status);
    
    List<Payment> findByStatusAndExpiresAtBefore(Payment.PaymentStatus status, LocalDateTime expiryDate);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    Long countByStatus(@Param("status") Payment.PaymentStatus status);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal getTotalRevenue();
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'COMPLETED' AND p.createdAt >= :startDate")
    List<Payment> findCompletedPaymentsAfter(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId AND p.status = 'COMPLETED'")
    List<Payment> findCompletedPaymentsByUser(@Param("userId") Long userId);
} 