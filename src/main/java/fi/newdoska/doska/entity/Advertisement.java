package fi.newdoska.doska.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "advertisements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Advertisement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Заголовок обязателен")
    @Size(max = 200, message = "Заголовок не должен превышать 200 символов")
    @Column(nullable = false)
    private String title;
    
    @NotBlank(message = "Описание обязательно")
    @Size(max = 5000, message = "Описание не должно превышать 5000 символов")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;
    
    @NotNull(message = "Категория обязательна")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private fi.newdoska.doska.entity.Subcategory subcategory;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdvertisementType type = AdvertisementType.SALE;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(length = 100)
    private String location;
    
    @Column(length = 100)
    private String city;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;
    
    @Column(nullable = false)
    private boolean isPremium = false;
    
    @Column
    private LocalDateTime premiumExpiresAt;
    
    @Column
    private Integer premiumDays;
    
    @Column(nullable = false)
    private boolean isUrgent = false;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime publishedAt;
    
    @Column
    private LocalDateTime expiresAt;
    
    @Column
    private Integer views = 0;
    
    @Column(nullable = false)
    private Boolean showPhone = false;

    @Column(length = 50)
    private String externalSource;

    @Column(length = 64)
    private String externalId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @OneToMany(mappedBy = "advertisement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AdvertisementImage> images;
    
    @OneToMany(mappedBy = "advertisement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum Category {
        REAL_ESTATE("Недвижимость"),
        VEHICLES("Транспорт"),
        ELECTRONICS("Электроника"),
        FURNITURE("Мебель"),
        CLOTHING("Одежда"),
        BOOKS("Книги"),
        SPORTS("Спорт"),
        SERVICES("Услуги"),
        JOBS("Работа"),
        OTHER("Другое");
        
        private final String displayName;
        
        Category(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum AdvertisementType {
        SALE("Продажа"),
        BUY("Покупка"),
        RENT("Аренда"),
        EXCHANGE("Обмен"),
        SERVICE("Услуга"),
        // Для работы
        FULL_TIME("Полная занятость"),
        PART_TIME("Частичная занятость"),
        CONTRACT("Контракт"),
        TEMPORARY("Временная работа"),
        INTERNSHIP("Стажировка"),
        REMOTE("Удаленная работа"),
        // Направление поиска
        JOB_SEEKING("Ищу работу"),
        JOB_OFFERING("Предлагаю работу");
        
        private final String displayName;
        
        AdvertisementType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum Status {
        PENDING("На модерации"),
        APPROVED("Одобрено"),
        REJECTED("Отклонено"),
        EXPIRED("Истекло"),
        DELETED("Удалено");
        
        private final String displayName;
        
        Status(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
} 