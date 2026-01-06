package fi.newdoska.doska.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "banners")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Banner {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private String imageUrl;
    
    @Column
    private String linkUrl;
    
    @Column(nullable = false)
    private String position; // TOP, SIDEBAR, BOTTOM
    
    @Column(nullable = false)
    private boolean active = true;
    
    @Column(nullable = false)
    private LocalDateTime startDate = LocalDateTime.now();
    
    @Column
    private LocalDateTime endDate;
    
    @Column(nullable = false)
    private Integer clicks = 0;
    
    @Column(nullable = false)
    private Integer views = 0;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

