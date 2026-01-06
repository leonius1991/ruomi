package fi.newdoska.doska.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "category_subscriptions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorySubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @Column(nullable = false)
    private LocalDateTime subscribedAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private Boolean active = true;
}

