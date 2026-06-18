package fi.newdoska.doska.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "visit_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitStatistics {

    @Id
    private Integer id;

    @Column(name = "total_visits", nullable = false)
    private Long totalVisits;

    @Column(name = "today_visits", nullable = false)
    private Long todayVisits;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
}
