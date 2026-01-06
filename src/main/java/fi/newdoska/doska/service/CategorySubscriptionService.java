package fi.newdoska.doska.service;

import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.entity.Category;
import fi.newdoska.doska.entity.CategorySubscription;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.CategoryRepository;
import fi.newdoska.doska.repository.CategorySubscriptionRepository;
import fi.newdoska.doska.telegram.VfinkeTelegramBot;
import fi.newdoska.doska.util.TextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CategorySubscriptionService {
    
    private final CategorySubscriptionRepository subscriptionRepository;
    private final CategoryRepository categoryRepository;
    
    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private VfinkeTelegramBot telegramBot;
    
    public CategorySubscription subscribe(User user, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
        
        Optional<CategorySubscription> existing = subscriptionRepository.findByUserAndCategory(user, category);
        
        if (existing.isPresent()) {
            CategorySubscription subscription = existing.get();
            if (!subscription.getActive()) {
                subscription.setActive(true);
                return subscriptionRepository.save(subscription);
            }
            return subscription;
        }
        
        CategorySubscription subscription = new CategorySubscription();
        subscription.setUser(user);
        subscription.setCategory(category);
        subscription.setActive(true);
        
        return subscriptionRepository.save(subscription);
    }
    
    public void unsubscribe(User user, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
        
        Optional<CategorySubscription> subscription = subscriptionRepository.findByUserAndCategory(user, category);
        if (subscription.isPresent()) {
            CategorySubscription sub = subscription.get();
            sub.setActive(false);
            subscriptionRepository.save(sub);
        }
    }
    
    public List<CategorySubscription> getUserSubscriptions(User user) {
        return subscriptionRepository.findByUserAndActiveTrue(user);
    }
    
    public List<CategorySubscription> getCategorySubscribers(Category category) {
        return subscriptionRepository.findByCategoryAndActiveTrue(category);
    }
    
    public long getCategorySubscriberCount(Category category) {
        return subscriptionRepository.countByCategoryAndActiveTrue(category);
    }
    
    public long getTotalSubscriberCount() {
        return subscriptionRepository.countByActiveTrue();
    }
    
    public boolean isSubscribed(User user, Category category) {
        Optional<CategorySubscription> subscription = subscriptionRepository.findByUserAndCategory(user, category);
        return subscription.isPresent() && subscription.get().getActive();
    }
    
    public void notifySubscribers(Advertisement advertisement) {
        // Находим Category entity по имени, соответствующему enum
        String categoryName = advertisement.getCategory().name();
        Optional<Category> categoryOpt = categoryRepository.findByName(categoryName);
        
        if (categoryOpt.isEmpty()) {
            log.warn("Category entity not found for enum: " + categoryName);
            return;
        }
        
        Category category = categoryOpt.get();
        List<CategorySubscription> subscriptions = getCategorySubscribers(category);
        
        if (subscriptions.isEmpty()) {
            return;
        }
        
        String message = String.format(
            "🔔 <b>Новое объявление в категории \"%s\"</b>\n\n" +
            "📋 <b>%s</b>\n" +
            "💰 Цена: %s €\n" +
            "📝 %s\n" +
            "👤 %s\n\n" +
            "🔗 Посмотреть: /ad_%d",
            category.getDisplayName(),
            advertisement.getTitle(),
            advertisement.getPrice() != null ? advertisement.getPrice() : "Не указана",
            TextUtils.abbreviate(advertisement.getDescription(), 100),
            advertisement.getUser().getUsername(),
            advertisement.getId()
        );
        
        int sent = 0;
        int failed = 0;
        
        for (CategorySubscription subscription : subscriptions) {
            User subscriber = subscription.getUser();
            // Не отправляем уведомление автору объявления
            if (subscriber.getId().equals(advertisement.getUser().getId())) {
                continue;
            }
            
            if (subscriber.getTelegramId() != null && telegramBot != null) {
                try {
                    telegramBot.sendMessage(subscriber.getTelegramId(), message);
                    sent++;
                } catch (Exception e) {
                    log.error("Failed to send notification to user " + subscriber.getId(), e);
                    failed++;
                }
            }
        }
        
        log.info("Sent {} notifications, {} failed for advertisement {}", sent, failed, advertisement.getId());
    }
}

