package br.com.accessmap.backend.review.model;

import br.com.accessmap.backend.review.enums.ReportReason;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "review_reports")
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String reviewId;

    @Column(nullable = false)
    private String reporterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    /** Quem denunciou já tinha passado do prazo de conta nova: só essas denúncias ocultam a review. */
    @Column(nullable = false)
    private boolean reporterEstablished;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
