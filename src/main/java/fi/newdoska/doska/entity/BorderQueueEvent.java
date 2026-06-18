package fi.newdoska.doska.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "border_queue_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorderQueueEvent {

    public enum EventType {
        ENTER, EXIT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String checkpoint;

    @Column(nullable = false, length = 16)
    private String lane;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 16)
    private EventType eventType;

    @Column(nullable = false)
    private Integer delta;

    @Column(name = "previous_count", nullable = false)
    private Integer previousCount;

    @Column(name = "new_count", nullable = false)
    private Integer newCount;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
