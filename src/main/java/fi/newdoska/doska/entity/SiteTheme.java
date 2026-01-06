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
    private String primaryColor = "#0d6efd";
    
    @Column(nullable = false)
    private String secondaryColor = "#6c757d";
    
    @Column(nullable = false)
    private String successColor = "#198754";
    
    @Column(nullable = false)
    private String dangerColor = "#dc3545";
    
    @Column(nullable = false)
    private String warningColor = "#ffc107";
    
    @Column(nullable = false)
    private String infoColor = "#0dcaf0";
    
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
    private String borderRadius = "0.375rem";
    
    @Column(nullable = false)
    private String boxShadow = "0 0.125rem 0.25rem rgba(0, 0, 0, 0.075)";
    
    @Column(nullable = false)
    private String navbarHeight = "56px";
    
    @Column(nullable = false)
    private String containerMaxWidth = "1320px";
    
    // Hero section gradient colors
    @Column(nullable = false)
    private String heroGradientStart = "#667eea";
    
    @Column(nullable = false)
    private String heroGradientEnd = "#764ba2";
}

