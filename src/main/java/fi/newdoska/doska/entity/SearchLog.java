package fi.newdoska.doska.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs")
@Getter
@Setter
@NoArgsConstructor
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String term;

    private String category;
    private String city;
    private Long resultsCount;
    private String username;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}


