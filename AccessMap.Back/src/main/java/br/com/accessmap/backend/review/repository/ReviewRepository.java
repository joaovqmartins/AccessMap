package br.com.accessmap.backend.review.repository;

import br.com.accessmap.backend.review.enums.AccessibilityTag;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.enums.TagAssessment;
import br.com.accessmap.backend.review.enums.TrustLevel;
import br.com.accessmap.backend.review.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, String> {

    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    Page<Review> findByPlaceIdAndStatus(String placeId, ReviewStatus status, Pageable pageable);

    Page<Review> findByUserIdAndStatus(String userId, ReviewStatus status, Pageable pageable);

    Page<Review> findByUserIdAndPlaceIdAndStatus(String userId, String placeId, ReviewStatus status, Pageable pageable);

    boolean existsByUserIdAndPlaceIdAndStatus(String userId, String placeId, ReviewStatus status);

    Optional<Review> findByIdAndStatus(String id, ReviewStatus status);

    /** Fila de moderação: {@code suspicionReason} preenchido é o que coloca a review nela. */
    Page<Review> findBySuspicionReasonIsNotNullAndStatusIn(Collection<ReviewStatus> statuses, Pageable pageable);

    /** Avaliações criadas pelo usuário desde {@code since}, em qualquer status: excluir e recriar conta. */
    long countByUserIdAndCreatedAtAfter(String userId, LocalDateTime since);

    long countByUserIdAndStatus(String userId, ReviewStatus status);

    long countByUserIdAndModerationRemovedAtAfter(String userId, LocalDateTime since);

    @Query("SELECT r.authorTrustLevel AS level, COUNT(r) AS reviewCount, SUM(r.rating) AS ratingSum FROM Review r " +
            "WHERE r.placeId = :placeId AND r.status = :status GROUP BY r.authorTrustLevel")
    List<LevelAggregate> aggregateByPlaceIdAndLevel(String placeId, ReviewStatus status);

    @Query("SELECT KEY(t) AS tag, VALUE(t) AS assessment, COUNT(r) AS total FROM Review r JOIN r.tags t " +
            "WHERE r.placeId = :placeId AND r.status = :status " +
            "GROUP BY KEY(t), VALUE(t)")
    List<TagAssessmentCount> tagStatsByPlaceId(String placeId, ReviewStatus status);

    interface LevelAggregate {
        TrustLevel getLevel();

        long getReviewCount();

        long getRatingSum();
    }

    interface TagAssessmentCount {
        AccessibilityTag getTag();

        TagAssessment getAssessment();

        long getTotal();
    }
}
