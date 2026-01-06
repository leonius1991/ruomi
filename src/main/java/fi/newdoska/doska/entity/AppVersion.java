package fi.newdoska.doska.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String version;
    
    @Column(nullable = false)
    private LocalDateTime installedAt = LocalDateTime.now();
    
    @Column
    private String releaseNotes;
    
    @Column
    private String downloadUrl;
    
    @Column(nullable = false)
    private Boolean isCurrent = false;
    
    @Column
    private String updateStatus; // SUCCESS, FAILED, IN_PROGRESS
    
    @Column
    private String errorMessage;
}

