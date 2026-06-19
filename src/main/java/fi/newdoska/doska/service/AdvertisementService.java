package fi.newdoska.doska.service;

import fi.newdoska.doska.dto.AdvertisementDto;
import fi.newdoska.doska.entity.Advertisement;
import fi.newdoska.doska.entity.AdvertisementImage;
import fi.newdoska.doska.entity.ModerationLog;
import fi.newdoska.doska.entity.User;
import fi.newdoska.doska.repository.AdvertisementImageRepository;
import fi.newdoska.doska.repository.AdvertisementRepository;
import fi.newdoska.doska.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdvertisementService {
    
    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementImageRepository imageRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ModerationLogService moderationLogService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final CategorySubscriptionService categorySubscriptionService;
    private final EmailService emailService;
    private final org.springframework.context.ApplicationContext applicationContext;
    private final fi.newdoska.doska.repository.SubcategoryRepository subcategoryRepository;
    
    public Advertisement createAdvertisement(AdvertisementDto dto, String username) {
        return createAdvertisement(dto, username, null);
    }
    
    public Advertisement createAdvertisement(AdvertisementDto dto, String username, Integer premiumDays) {
        User user = (User) userService.loadUserByUsername(username);
        
        Advertisement advertisement = new Advertisement();
        advertisement.setTitle(dto.getTitle());
        advertisement.setDescription(dto.getDescription());
        advertisement.setCategory(Advertisement.Category.valueOf(dto.getCategory()));
        
        // Устанавливаем подкатегорию, если указана
        if (dto.getSubcategoryId() != null) {
            fi.newdoska.doska.entity.Subcategory subcategory = 
                subcategoryRepository.findById(dto.getSubcategoryId()).orElse(null);
            advertisement.setSubcategory(subcategory);
        }
        
        advertisement.setType(Advertisement.AdvertisementType.valueOf(dto.getType()));
        advertisement.setPrice(dto.getPrice());
        advertisement.setLocation(dto.getLocation());
        advertisement.setCity(dto.getCity());
        advertisement.setStatus(Advertisement.Status.PENDING);
        advertisement.setPremium(dto.isPremium());
        advertisement.setUrgent(dto.isUrgent());
        advertisement.setShowPhone(dto.getShowPhone() != null ? dto.getShowPhone() : false);
        advertisement.setUser(user);
        advertisement.setCreatedAt(LocalDateTime.now());
        
        // Обработка премиум размещения
        if (premiumDays != null && premiumDays > 0 && dto.isPremium()) {
            advertisement.setPremiumDays(premiumDays);
            advertisement.setPremiumExpiresAt(LocalDateTime.now().plusDays(premiumDays));
        }
        
        // Устанавливаем срок действия объявления (30 дней по умолчанию)
        advertisement.setExpiresAt(LocalDateTime.now().plusDays(30));
        
        Advertisement savedAd = advertisementRepository.save(advertisement);
        
        // Обновляем счетчик объявлений пользователя
        user.setAdvertisementsCount((user.getAdvertisementsCount() != null ? user.getAdvertisementsCount() : 0) + 1);
        userService.saveUser(user);
        
        // Отправляем уведомления подписчикам категории
        try {
            categorySubscriptionService.notifySubscribers(savedAd);
        } catch (Exception e) {
            log.error("Failed to notify category subscribers", e);
        }
        
        // Отправляем уведомления модераторам и администраторам
        try {
            notifyModeratorsAboutNewAdvertisement(savedAd);
        } catch (Exception e) {
            log.error("Failed to notify moderators about new advertisement", e);
        }
        
        // Сохранение изображений
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            try {
                List<String> fileNames = fileStorageService.storeFiles(dto.getImages());
                for (int i = 0; i < fileNames.size(); i++) {
                    AdvertisementImage image = new AdvertisementImage();
                    image.setFileName(fileNames.get(i));
                    image.setOriginalFileName(dto.getImages().get(i).getOriginalFilename());
                    image.setFilePath("/files/" + fileNames.get(i));
                    image.setContentType(dto.getImages().get(i).getContentType());
                    image.setFileSize(dto.getImages().get(i).getSize());
                    image.setMain(i == 0);
                    image.setAdvertisement(savedAd);
                    imageRepository.save(image);
                }
            } catch (Exception e) {
                log.error("Ошибка при сохранении изображений", e);
            }
        }
        
        return savedAd;
    }
    
    public Advertisement updateAdvertisement(Long id, AdvertisementDto dto, String username) {
        User user = (User) userService.loadUserByUsername(username);
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Объявление не найдено"));
        
        // Проверяем, что пользователь является владельцем объявления или модератором/админом
        if (!advertisement.getUser().getId().equals(user.getId()) && !hasModeratorPrivileges(user)) {
            throw new RuntimeException("У вас нет прав для редактирования этого объявления");
        }
        
        advertisement.setTitle(dto.getTitle());
        advertisement.setDescription(dto.getDescription());
        advertisement.setCategory(Advertisement.Category.valueOf(dto.getCategory()));
        
        // Устанавливаем подкатегорию, если указана
        if (dto.getSubcategoryId() != null) {
            fi.newdoska.doska.entity.Subcategory subcategory = 
                subcategoryRepository.findById(dto.getSubcategoryId()).orElse(null);
            advertisement.setSubcategory(subcategory);
        } else {
            advertisement.setSubcategory(null);
        }
        
        advertisement.setType(Advertisement.AdvertisementType.valueOf(dto.getType()));
        advertisement.setPrice(dto.getPrice());
        advertisement.setLocation(dto.getLocation());
        advertisement.setCity(dto.getCity());
        advertisement.setPremium(dto.isPremium());
        advertisement.setUrgent(dto.isUrgent());
        advertisement.setShowPhone(dto.getShowPhone() != null ? dto.getShowPhone() : false);
        advertisement.setUpdatedAt(LocalDateTime.now());
        
        // Если редактирует не модератор/админ, то объявление снова идет на модерацию
        if (!hasModeratorPrivileges(user)) {
            advertisement.setStatus(Advertisement.Status.PENDING);
        }
        
        return advertisementRepository.save(advertisement);
    }
    
    public void deleteAdvertisement(Long id, String username) {
        User user = (User) userService.loadUserByUsername(username);
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Объявление не найдено"));
        
        // Проверяем права на удаление
        if (!advertisement.getUser().getId().equals(user.getId()) && !hasModeratorPrivileges(user)) {
            throw new RuntimeException("У вас нет прав для удаления этого объявления");
        }
        
        advertisement.setStatus(Advertisement.Status.DELETED);
        advertisementRepository.save(advertisement);
    }
    
    public Page<Advertisement> getApprovedAdvertisements(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("isPremium").descending()
                .and(Sort.by("isUrgent").descending())
                .and(Sort.by("publishedAt").descending()));
        return advertisementRepository.findApprovedAdvertisements(pageable);
    }
    
    public Page<Advertisement> getAdvertisementsByCategory(String category, int page, int size) {
        Advertisement.Category cat = Advertisement.Category.valueOf(category.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by("isPremium").descending()
                .and(Sort.by("isUrgent").descending())
                .and(Sort.by("publishedAt").descending()));
        return advertisementRepository.findApprovedAdvertisementsByCategory(cat, pageable);
    }
    
    public Page<Advertisement> getAdvertisementsByCategoryAndType(String category, String type, int page, int size) {
        Advertisement.Category cat = Advertisement.Category.valueOf(category.toUpperCase());
        Advertisement.AdvertisementType adType = Advertisement.AdvertisementType.valueOf(type.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by("isPremium").descending()
                .and(Sort.by("isUrgent").descending())
                .and(Sort.by("publishedAt").descending()));
        return advertisementRepository.findByStatusAndCategoryAndType(
                Advertisement.Status.APPROVED, cat, adType, pageable);
    }
    
    public Page<Advertisement> getAdvertisementsByCity(String city, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("isPremium").descending()
                .and(Sort.by("isUrgent").descending())
                .and(Sort.by("publishedAt").descending()));
        return advertisementRepository.findByCity(city, pageable);
    }
    
    public Page<Advertisement> searchAdvertisements(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("isPremium").descending()
                .and(Sort.by("isUrgent").descending())
                .and(Sort.by("publishedAt").descending()));
        return advertisementRepository.searchApprovedAdvertisements(searchTerm, pageable);
    }
    
    public Page<Advertisement> getAdvertisementsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("isPremium").descending()
                .and(Sort.by("isUrgent").descending())
                .and(Sort.by("publishedAt").descending()));
        return advertisementRepository.findAdvertisementsByPriceRange(minPrice, maxPrice, pageable);
    }
    
    public Page<Advertisement> getAdvertisementsByCityAndCategory(String city, String category, int page, int size) {
        Advertisement.Category cat = Advertisement.Category.valueOf(category.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by("isPremium").descending()
                .and(Sort.by("isUrgent").descending())
                .and(Sort.by("publishedAt").descending()));
        return advertisementRepository.findAdvertisementsByCityAndCategory(city, cat, pageable);
    }
    
    public Page<Advertisement> getUserAdvertisements(String username, int page, int size) {
        User user = (User) userService.loadUserByUsername(username);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return advertisementRepository.findByUserId(user.getId(), pageable);
    }
    
    public Page<Advertisement> getPendingAdvertisements(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return advertisementRepository.findByStatus(Advertisement.Status.PENDING, pageable);
    }
    
    public Optional<Advertisement> getAdvertisementById(Long id) {
        return advertisementRepository.findById(id);
    }
    
    public List<Advertisement> getPremiumAdvertisements() {
        return advertisementRepository.findPremiumAdvertisements();
    }
    
    public List<Advertisement> getUrgentAdvertisements() {
        return advertisementRepository.findUrgentAdvertisements();
    }
    
    public void approveAdvertisement(Long id, String moderatorUsername, String comment) {
        User moderator = (User) userService.loadUserByUsername(moderatorUsername);
        if (!hasModeratorPrivileges(moderator)) {
            throw new RuntimeException("У вас нет прав для модерации объявлений");
        }
        
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Объявление не найдено"));
        
        advertisement.setStatus(Advertisement.Status.APPROVED);
        advertisement.setPublishedAt(LocalDateTime.now());
        advertisementRepository.save(advertisement);
        
        moderationLogService.logAction(advertisement, moderator, ModerationLog.Action.APPROVED, comment);
        notificationService.notifyNewAdvertisement(advertisement.getUser(), advertisement.getTitle());
    }
    
    public void rejectAdvertisement(Long id, String reason, String moderatorUsername) {
        User moderator = (User) userService.loadUserByUsername(moderatorUsername);
        if (!hasModeratorPrivileges(moderator)) {
            throw new RuntimeException("У вас нет прав для модерации объявлений");
        }
        
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Объявление не найдено"));
        
        advertisement.setStatus(Advertisement.Status.REJECTED);
        advertisementRepository.save(advertisement);
        
        moderationLogService.logAction(advertisement, moderator, ModerationLog.Action.REJECTED, reason);
        notificationService.notifyAdvertisementRejected(advertisement.getUser(), advertisement.getTitle(), reason);
    }
    
    private void notifyModeratorsAboutNewAdvertisement(Advertisement advertisement) {
        try {
            List<User> moderators = userRepository.findAllModeratorsAndAdmins();
            
            for (User moderator : moderators) {
                // Email уведомление
                try {
                    emailService.sendModerationNotification(moderator, advertisement);
                } catch (Exception e) {
                    log.error("Failed to send email notification to moderator {}", moderator.getId(), e);
                }
                
                // Telegram уведомление
                if (moderator.getTelegramId() != null) {
                    try {
                        // Получаем бота из контекста, чтобы избежать циклических зависимостей
                        fi.newdoska.doska.telegram.VfinkeTelegramBot telegramBot = 
                            applicationContext.getBean(fi.newdoska.doska.telegram.VfinkeTelegramBot.class);
                        
                        if (telegramBot != null) {
                            String telegramMessage = String.format(
                                "🔔 <b>Новое объявление требует модерации</b>\n\n" +
                                "📋 <b>%s</b>\n" +
                                "📂 Категория: %s\n" +
                                "👤 Автор: %s %s (@%s)\n" +
                                "💰 Цена: %s\n" +
                                "📍 Город: %s\n\n" +
                                "📝 %s\n\n" +
                                "🔗 <a href=\"https://ruomi.fi/moderator/dashboard\">Перейти к модерации</a>",
                                advertisement.getTitle(),
                                advertisement.getCategory().getDisplayName(),
                                advertisement.getUser().getFirstName(),
                                advertisement.getUser().getLastName(),
                                advertisement.getUser().getUsername(),
                                advertisement.getPrice() != null ? advertisement.getPrice() + " €" : "Не указана",
                                advertisement.getCity() != null ? advertisement.getCity() : "Не указан",
                                advertisement.getDescription().length() > 150 
                                    ? advertisement.getDescription().substring(0, 150) + "..." 
                                    : advertisement.getDescription()
                            );
                            telegramBot.sendMessage(moderator.getTelegramId(), telegramMessage);
                        }
                    } catch (Exception e) {
                        log.error("Failed to send Telegram notification to moderator {}", moderator.getId(), e);
                    }
                }
            }
            
            log.info("Notified {} moderators about new advertisement {}", moderators.size(), advertisement.getId());
        } catch (Exception e) {
            log.error("Error notifying moderators about new advertisement", e);
        }
    }
    
    public void incrementViews(Long id) {
        advertisementRepository.findById(id).ifPresent(advertisement -> {
            advertisement.setViews(advertisement.getViews() + 1);
            advertisementRepository.save(advertisement);
        });
    }
    
    public Long getTotalApprovedAdvertisementsCount() {
        return advertisementRepository.countApprovedAdvertisements();
    }
    
    public Long getPendingAdvertisementsCount() {
        return advertisementRepository.countPendingAdvertisements();
    }
    
    public void checkExpiredAdvertisements() {
        List<Advertisement> expiredAdvertisements = advertisementRepository.findExpiringAdvertisements(LocalDateTime.now());
        for (Advertisement advertisement : expiredAdvertisements) {
            advertisement.setStatus(Advertisement.Status.EXPIRED);
            advertisementRepository.save(advertisement);
        }
    }
    
    public List<Advertisement> getAllAdvertisements() {
        // Загружаем объявления с пользователями через JOIN FETCH для избежания LazyInitializationException
        return advertisementRepository.findAll().stream()
                .map(ad -> {
                    // Инициализируем прокси User, вызывая getUsername
                    if (ad.getUser() != null) {
                        try {
                            ad.getUser().getUsername();
                        } catch (Exception e) {
                            log.warn("Error initializing user for advertisement {}: {}", ad.getId(), e.getMessage());
                        }
                    }
                    return ad;
                })
                .toList();
    }
    
    public List<Advertisement> getAdvertisementsByUser(User user) {
        // Загружаем объявления и инициализируем User прокси для избежания LazyInitializationException
        return advertisementRepository.findByUserId(user.getId()).stream()
                .map(ad -> {
                    // Инициализируем прокси User, вызывая getUsername
                    if (ad.getUser() != null) {
                        try {
                            ad.getUser().getUsername();
                        } catch (Exception e) {
                            log.warn("Error initializing user for advertisement {}: {}", ad.getId(), e.getMessage());
                        }
                    }
                    return ad;
                })
                .toList();
    }
    
    public Advertisement saveAdvertisement(Advertisement advertisement) {
        return advertisementRepository.save(advertisement);
    }

    public Page<Advertisement> getAdvertisementsForAdmin(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (search != null && !search.isBlank()) {
            return advertisementRepository.adminSearch(search.trim(), pageable);
        }
        return advertisementRepository.findAllForAdmin(pageable);
    }

    public Advertisement adminUpdateAdvertisement(Long id, AdvertisementDto dto) {
        Advertisement ad = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Объявление не найдено"));

        ad.setTitle(dto.getTitle());
        ad.setDescription(dto.getDescription());
        ad.setCategory(Advertisement.Category.valueOf(dto.getCategory()));
        if (dto.getSubcategoryId() != null) {
            ad.setSubcategory(subcategoryRepository.findById(dto.getSubcategoryId()).orElse(null));
        } else {
            ad.setSubcategory(null);
        }
        ad.setType(Advertisement.AdvertisementType.valueOf(dto.getType()));
        ad.setPrice(dto.getPrice());
        ad.setLocation(dto.getLocation());
        ad.setCity(dto.getCity());
        ad.setPremium(dto.isPremium());
        ad.setUrgent(dto.isUrgent());
        ad.setShowPhone(dto.getShowPhone() != null ? dto.getShowPhone() : false);

        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            ad.setStatus(Advertisement.Status.valueOf(dto.getStatus()));
        }
        if (dto.getUserId() != null) {
            User owner = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
            ad.setUser(owner);
        }
        ad.setUpdatedAt(LocalDateTime.now());
        return advertisementRepository.save(ad);
    }

    public void adminDeleteAdvertisement(Long id) {
        Advertisement ad = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Объявление не найдено"));
        ad.setStatus(Advertisement.Status.DELETED);
        advertisementRepository.save(ad);
    }

    public int adminBulkDeleteAdvertisements(List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            try {
                adminDeleteAdvertisement(id);
                count++;
            } catch (Exception e) {
                log.warn("Failed to delete ad {}: {}", id, e.getMessage());
            }
        }
        return count;
    }
    
    private boolean hasModeratorPrivileges(User user) {
        return user.getRole().equals(User.UserRole.MODERATOR)
                || user.getRole().equals(User.UserRole.ADMIN)
                || user.getRole().equals(User.UserRole.SUPER_ADMIN);
    }
}