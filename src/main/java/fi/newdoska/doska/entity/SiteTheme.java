package fi.newdoska.doska.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "site_theme")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteTheme {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = 1L;
    
    @Column(nullable = false)
    private String primaryColor = "#0E7C66";
    
    @Column(nullable = false)
    private String secondaryColor = "#6B7C75";
    
    @Column(nullable = false)
    private String successColor = "#2E9E6B";
    
    @Column(nullable = false)
    private String dangerColor = "#E0573F";
    
    @Column(nullable = false)
    private String warningColor = "#E0A23B";
    
    @Column(nullable = false)
    private String infoColor = "#2A9D8F";
    
    // Размеры текста
    @Column(nullable = false)
    private String baseFontSize = "16px";
    
    @Column(nullable = false)
    private String headingFontSize = "2rem";
    
    @Column(nullable = false)
    private String smallFontSize = "0.875rem";
    
    @Column(nullable = false)
    private String largeFontSize = "1.25rem";
    
    // Дополнительные настройки
    @Column(nullable = false)
    private String borderRadius = "0.85rem";
    
    @Column(nullable = false)
    private String boxShadow = "0 0.125rem 0.5rem rgba(31, 61, 53, 0.08)";
    
    @Column(nullable = false)
    private String navbarHeight = "68px";
    
    @Column(nullable = false)
    private String containerMaxWidth = "1200px";
    
    // Hero section gradient colors
    @Column(nullable = false)
    private String heroGradientStart = "#0E7C66";
    
    @Column(nullable = false)
    private String heroGradientEnd = "#2E9E6B";
}

