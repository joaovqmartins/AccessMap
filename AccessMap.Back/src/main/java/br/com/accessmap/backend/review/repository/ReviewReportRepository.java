package br.com.accessmap.backend.review.repository;

import br.com.accessmap.backend.review.model.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, String> {

    boolean existsByReviewIdAndReporterId(String reviewId, String reporterId);

    /** Total de denúncias da avaliação; é o que o cálculo de confiabilidade (#28) vai consumir. */
    long countByReviewId(String reviewId);
}
