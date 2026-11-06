package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.review.dto.AdminReviewDto;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.repository.ReviewReportRepository;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/** Fila de moderação: reviews ocultas por denúncias e publicadas com nota destoante (#27). */
@Service
@RequiredArgsConstructor
public class ReviewModerationService {

    private static final List<ReviewStatus> IN_QUEUE = List.of(ReviewStatus.PUBLICADA, ReviewStatus.OCULTA);

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final ReviewService reviewService;

    public Page<AdminReviewDto> queue(Pageable pageable) {
        return reviewRepository.findBySuspicionReasonIsNotNullAndStatusIn(IN_QUEUE, pageable)
                .map(review -> AdminReviewDto.from(review, reviewReportRepository.countByReviewId(review.getId())));
    }

    /** Volta ao ar e fica protegida: o ADMIN já julgou as denúncias, que não ocultam de novo. */
    @Transactional
    public void restore(String id) {
        Review review = find(id);
        if (review.getStatus() != ReviewStatus.OCULTA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta avaliação não está oculta");
        }

        review.setStatus(ReviewStatus.PUBLICADA);
        review.setSuspicionReason(null);
        review.setModerationRestoredAt(LocalDateTime.now());
        saveAndRecalculate(review);
    }

    /** Só tira da fila. O ADMIN julgou a nota, não denúncias: se elas vierem depois, ainda ocultam. */
    @Transactional
    public void keep(String id) {
        Review review = find(id);
        if (review.getStatus() != ReviewStatus.PUBLICADA || review.getSuspicionReason() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta avaliação não está aguardando moderação");
        }

        review.setSuspicionReason(null);
        review.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }

    /** Soft delete que conta para o nível Restrita do autor (#28). */
    @Transactional
    public void remove(String id) {
        Review review = find(id);
        if (review.getStatus() == ReviewStatus.REMOVIDA) {
            throw notFound();
        }

        review.setStatus(ReviewStatus.REMOVIDA);
        review.setSuspicionReason(null);
        review.setModerationRemovedAt(LocalDateTime.now());
        saveAndRecalculate(review);
    }

    private Review find(String id) {
        return reviewRepository.findById(id).orElseThrow(ReviewModerationService::notFound);
    }

    private void saveAndRecalculate(Review review) {
        review.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(review);
        reviewService.recalculatePlaceAggregates(review.getPlaceId());
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada");
    }
}
