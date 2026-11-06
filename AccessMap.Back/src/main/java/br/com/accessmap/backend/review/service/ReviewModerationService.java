package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.review.dto.AdminReviewDto;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.repository.ReviewReportRepository;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/** Fila de moderação: reviews ocultas por denúncias e publicadas com nota destoante (#27). */
@Service
@RequiredArgsConstructor
public class ReviewModerationService {

    private static final List<ReviewStatus> IN_QUEUE = List.of(ReviewStatus.PUBLICADA, ReviewStatus.OCULTA);

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;

    public Page<AdminReviewDto> queue(Pageable pageable) {
        return reviewRepository.findBySuspicionReasonIsNotNullAndStatusIn(IN_QUEUE, pageable)
                .map(review -> AdminReviewDto.from(review, reviewReportRepository.countByReviewId(review.getId())));
    }
}
