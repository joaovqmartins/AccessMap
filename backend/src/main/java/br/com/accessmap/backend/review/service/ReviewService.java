package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.service.UserService;
import br.com.accessmap.backend.place.model.TagStats;
import br.com.accessmap.backend.place.service.PlaceService;
import br.com.accessmap.backend.review.dto.ReviewRequestDto;
import br.com.accessmap.backend.review.dto.ReviewUpdateRequestDto;
import br.com.accessmap.backend.review.enums.AccessibilityTag;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PlaceService placeService;
    private final UserService userService;

    public Page<Review> findAll(Pageable pageable) {
        return reviewRepository.findByStatus(ReviewStatus.PUBLICADA, pageable);
    }

    public Page<Review> findByPlaceId(String placeId, Pageable pageable) {
        return reviewRepository.findByPlaceIdAndStatus(placeId, ReviewStatus.PUBLICADA, pageable);
    }

    public Page<Review> findByUserId(String userId, Pageable pageable) {
        return reviewRepository.findByUserIdAndStatus(userId, ReviewStatus.PUBLICADA, pageable);
    }

    public Page<Review> findByUserIdAndPlaceId(String userId, String placeId, Pageable pageable) {
        return reviewRepository.findByUserIdAndPlaceIdAndStatus(userId, placeId, ReviewStatus.PUBLICADA, pageable);
    }

    public Review findById(String id) {
        return reviewRepository.findByIdAndStatus(id, ReviewStatus.PUBLICADA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));
    }

    /** @param userId autor da avaliação, extraído do token — nunca do corpo da requisição. */
    @Transactional
    public Review create(String userId, ReviewRequestDto request) {
        User author = userService.findById(userId);

        if (reviewRepository.existsByUserIdAndPlaceIdAndStatus(
                userId, request.getPlaceId(), ReviewStatus.PUBLICADA)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Você já avaliou este local. Edite sua avaliação existente."
            );
        }

        placeService.findOrCreateByPlaceId(request.getPlaceId());

        Review review = Review.builder()
                .userId(userId)
                .placeId(request.getPlaceId())
                .rating(request.getRating())
                .comment(request.getComment())
                .tags(request.getTags())
                .reviewerNeeds(author.getAccessibilityNeeds() == null
                        ? null
                        : new HashSet<>(author.getAccessibilityNeeds()))
                .status(ReviewStatus.PUBLICADA)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Review saved = reviewRepository.save(review);
        recalculatePlaceAggregates(request.getPlaceId());

        return saved;
    }

    /**
     * @param userId  quem está pedindo a alteração (subject do token)
     * @param isAdmin se true, pode alterar avaliações de outros usuários (moderação)
     */
    @Transactional
    public Review update(String id, String userId, boolean isAdmin, ReviewUpdateRequestDto request) {
        Review existing = findById(id);
        assertOwnerOrAdmin(existing, userId, isAdmin);

        if (request.getTags() != null && request.getTags().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe ao menos uma característica de acessibilidade"
            );
        }

        if (request.getRating() != null) existing.setRating(request.getRating());
        if (request.getComment() != null) existing.setComment(request.getComment());
        if (request.getTags() != null) existing.setTags(request.getTags());
        existing.setUpdatedAt(LocalDateTime.now());

        Review saved = reviewRepository.save(existing);
        recalculatePlaceAggregates(existing.getPlaceId());

        return saved;
    }

    @Transactional
    public void delete(String id, String userId, boolean isAdmin) {
        Review existing = findById(id);
        assertOwnerOrAdmin(existing, userId, isAdmin);

        existing.setStatus(ReviewStatus.REMOVIDA);
        existing.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(existing);

        recalculatePlaceAggregates(existing.getPlaceId());
    }

    private void assertOwnerOrAdmin(Review review, String userId, boolean isAdmin) {
        if (!isAdmin && !review.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não pode alterar a avaliação de outro usuário"
            );
        }
    }

    /**
     * Recalcula os agregados do local a partir das avaliações publicadas, em vez de somar
     * incrementalmente — assim o total nunca diverge, mesmo após edições e remoções.
     */
    private void recalculatePlaceAggregates(String placeId) {
        ReviewRepository.PlaceAggregate aggregate =
                reviewRepository.aggregateByPlaceId(placeId, ReviewStatus.PUBLICADA);

        Map<AccessibilityTag, TagStats> tagStats = new EnumMap<>(AccessibilityTag.class);

        reviewRepository.tagStatsByPlaceId(placeId, ReviewStatus.PUBLICADA).forEach(row -> {
            TagStats stats = tagStats.computeIfAbsent(row.getTag(), tag -> TagStats.builder().build());
            switch (row.getAssessment()) {
                case ADEQUADO -> stats.setAdequadoCount(row.getTotal());
                case INADEQUADO -> stats.setInadequadoCount(row.getTotal());
                case INEXISTENTE -> stats.setInexistenteCount(row.getTotal());
            }
        });

        placeService.applyAggregates(placeId, aggregate.getReviewCount(), aggregate.getRatingSum(), tagStats);
    }
}
