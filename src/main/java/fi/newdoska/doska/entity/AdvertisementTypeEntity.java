package fi.newdoska.doska.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "advertisement_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementTypeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String displayName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // null означает, что тип доступен для всех категорий
    
    @Column
    private Integer sortOrder = 0;
    
    @Column(nullable = false)
    private Boolean active = true;
}


