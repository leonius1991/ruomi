package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.entity.Payment;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final AdvertisementService advertisementService;
    
    public Payment createPayment(Long advertisementId, Payment.PaymentType type, String username) {
        User user = (User) userService.loadUserByUsername(username);
        Advertisement advertisement = advertisementService.getAdvertisementById(advertisementId)
                .orElseThrow(() -> new RuntimeException("Объявление не найдено"));
        
        // Проверяем, что пользователь является владельцем объявления
        if (!advertisement.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("У вас нет прав для оплаты этого объявления");
        }
        
        Payment payment = new Payment();
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setType(type);
        payment.setUser(user);
        payment.setAdvertisement(advertisement);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        
        // Устанавливаем сумму и описание в зависимости от типа платежа
        switch (type) {
            case PREMIUM_UPGRADE:
                payment.setAmount(new BigDecimal("19.99"));
                payment.setDescription("Премиум размещение объявления: " + advertisement.getTitle());
                payment.setExpiresAt(LocalDateTime.now().plusDays(30));
                break;
            case URGENT_UPGRADE:
                payment.setAmount(new BigDecimal("9.99"));
                payment.setDescription("Срочное размещение объявления: " + advertisement.getTitle());
                payment.setExpiresAt(LocalDateTime.now().plusDays(7));
                break;
            case TOP_PLACEMENT:
                payment.setAmount(new BigDecimal("4.99"));
                payment.setDescription("Поднятие объявления в топ: " + advertisement.getTitle());
                payment.setExpiresAt(LocalDateTime.now().plusDays(1));
                break;
            case EXTENDED_DURATION:
                payment.setAmount(new BigDecimal("14.99"));
                payment.setDescription("Продление срока объявления: " + advertisement.getTitle());
                payment.setExpiresAt(LocalDateTime.now().plusDays(30));
                break;
        }
        
        return paymentRepository.save(payment);
    }
    
    public Payment processPayment(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Платеж не найден"));
        
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new RuntimeException("Платеж уже обработан");
        }
        
        if (payment.getExpiresAt() != null && payment.getExpiresAt().isBefore(LocalDateTime.now())) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Время действия платежа истекло");
        }
        
        // Обрабатываем платеж в зависимости от типа
        Advertisement advertisement = payment.getAdvertisement();
        
        switch (payment.getType()) {
            case PREMIUM_UPGRADE:
                advertisement.setPremium(true);
                advertisement.setExpiresAt(LocalDateTime.now().plusDays(30));
                break;
            case URGENT_UPGRADE:
                advertisement.setUrgent(true);
                advertisement.setExpiresAt(LocalDateTime.now().plusDays(7));
                break;
            case TOP_PLACEMENT:
                // Поднимаем объявление в топ (обновляем время публикации)
                advertisement.setPublishedAt(LocalDateTime.now());
                break;
            case EXTENDED_DURATION:
                // Продлеваем срок действия на 30 дней
                if (advertisement.getExpiresAt() != null) {
                    advertisement.setExpiresAt(advertisement.getExpiresAt().plusDays(30));
                } else {
                    advertisement.setExpiresAt(LocalDateTime.now().plusDays(30));
                }
                break;
        }
        
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        
        // Сохраняем изменения в объявлении
        advertisementService.updateAdvertisement(advertisement.getId(), null, advertisement.getUser().getUsername());
        
        return paymentRepository.save(payment);
    }
    
    public void cancelPayment(String transactionId, String username) {
        User user = (User) userService.loadUserByUsername(username);
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Платеж не найден"));
        
        // Проверяем права на отмену
        if (!payment.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("У вас нет прав для отмены этого платежа");
        }
        
        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            throw new RuntimeException("Нельзя отменить уже выполненный платеж");
        }
        
        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        paymentRepository.save(payment);
    }
    
    public List<Payment> getUserPayments(String username) {
        User user = (User) userService.loadUserByUsername(username);
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
    
    public List<Payment> getAdvertisementPayments(Long advertisementId) {
        return paymentRepository.findByAdvertisementIdOrderByCreatedAtDesc(advertisementId);
    }
    
    public Optional<Payment> getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }
    
    public List<Payment> getPendingPayments() {
        return paymentRepository.findByStatus(Payment.PaymentStatus.PENDING);
    }
    
    public List<Payment> getCompletedPayments() {
        return paymentRepository.findByStatus(Payment.PaymentStatus.COMPLETED);
    }
    
    public BigDecimal getTotalRevenue() {
        return paymentRepository.findByStatus(Payment.PaymentStatus.COMPLETED)
                .stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public Long getTotalPaymentsCount() {
        return paymentRepository.count();
    }
    
    public Long getCompletedPaymentsCount() {
        return paymentRepository.countByStatus(Payment.PaymentStatus.COMPLETED);
    }
    
    public void cleanupExpiredPayments() {
        List<Payment> expiredPayments = paymentRepository.findByStatusAndExpiresAtBefore(
                Payment.PaymentStatus.PENDING, LocalDateTime.now());
        
        for (Payment payment : expiredPayments) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
    }
} 