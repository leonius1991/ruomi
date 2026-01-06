package fi.newdoska.doska.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "private_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;
    
    @NotBlank(message = "Текст сообщения обязателен")
    @Size(max = 2000, message = "Сообщение не должно превышать 2000 символов")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();
    
    @Column(name = "`read`", nullable = false)
    private Boolean read = false;
    
    @Column
    private LocalDateTime readAt;
    
    @Column
    private Boolean deletedBySender = false;
    
    @Column
    private Boolean deletedByRecipient = false;
}

