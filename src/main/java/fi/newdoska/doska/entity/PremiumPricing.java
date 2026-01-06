package fi.newdoska.doska.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "premium_pricing")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PremiumPricing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = 1L;
    
    @Column(nullable = false)
    private Integer days7 = 7;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price7 = BigDecimal.valueOf(5.00);
    
    @Column(nullable = false)
    private Integer days14 = 14;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price14 = BigDecimal.valueOf(9.00);
    
    @Column(nullable = false)
    private Integer days30 = 30;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price30 = BigDecimal.valueOf(15.00);
    
    @Column(nullable = false)
    private Integer days60 = 60;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price60 = BigDecimal.valueOf(25.00);
    
    @Column(nullable = false)
    private Integer days90 = 90;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price90 = BigDecimal.valueOf(35.00);
}


