package fi.newdoska.doska.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "broadcast_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;
    
    @NotBlank(message = "Текст сообщения обязателен")
    @Size(max = 4000, message = "Сообщение не должно превышать 4000 символов")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime sentAt;
    
    @Column(nullable = false)
    private Boolean sent = false;
    
    @Column
    private Integer totalRecipients = 0;
    
    @Column
    private Integer successfulSends = 0;
    
    @Column
    private Integer failedSends = 0;
}

