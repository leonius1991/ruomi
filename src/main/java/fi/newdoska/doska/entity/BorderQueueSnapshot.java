package fi.newdoska.doska.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "border_queue_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorderQueueSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String checkpoint;

    @Column(nullable = false, length = 16)
    private String lane;

    @Column(name = "live_count", nullable = false)
    private Integer liveCount;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;
}
