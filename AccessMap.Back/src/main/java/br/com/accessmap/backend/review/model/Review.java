package br.com.accessmap.backend.review.model;

import br.com.accessmap.backend.identity.enums.AccessibilityNeed;
import br.com.accessmap.backend.review.enums.AccessibilityTag;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.enums.SuspicionReason;
import br.com.accessmap.backend.review.enums.TagAssessment;
import br.com.accessmap.backend.review.enums.TrustLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String placeId;

    @Column(nullable = false)
    private Integer rating;

    private String comment;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "review_tags", joinColumns = @JoinColumn(name = "review_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "tag")
    @Enumerated(EnumType.STRING)
    @Column(name = "assessment")
    private Map<AccessibilityTag, TagAssessment> tags;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "review_reviewer_needs", joinColumns = @JoinColumn(name = "review_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "need")
    private Set<AccessibilityNeed> reviewerNeeds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PUBLICADA;

    /** Nível da conta quando a review foi criada: não muda depois, para não recalcular notas antigas. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TrustLevel authorTrustLevel = TrustLevel.REGULAR;

    /** Preenchido só quando um ADMIN remove a review de outra pessoa: conta para o nível Restrita. */
    private LocalDateTime moderationRemovedAt;

    /** Preenchido enquanto a review está na fila do ADMIN. */
    @Enumerated(EnumType.STRING)
    private SuspicionReason suspicionReason;

    /** Preenchido quando o ADMIN restaura: denúncias não ocultam de novo. */
    private LocalDateTime moderationRestoredAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
