package fi.newdoska.doska.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdvertisementDto {
    
    private Long id;
    
    @NotBlank(message = "Заголовок обязателен")
    @Size(max = 200, message = "Заголовок не должен превышать 200 символов")
    private String title;
    
    @NotBlank(message = "Описание обязательно")
    @Size(max = 5000, message = "Описание не должно превышать 5000 символов")
    private String description;
    
    @NotNull(message = "Категория обязательна")
    private String category;
    
    private String type = "SALE";
    
    private BigDecimal price;
    
    private String location;
    
    private String city;
    
    private String status;
    private boolean premium = false;
    private boolean urgent = false;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    
    private Integer views = 0;
    
    private Long userId;
    private String userUsername;
    private String userFirstName;
    private String userLastName;
    
    private List<MultipartFile> images;
    private List<String> existingImages;
} 