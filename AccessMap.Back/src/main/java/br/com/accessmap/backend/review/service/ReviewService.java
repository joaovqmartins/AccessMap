package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.service.UserService;
import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.model.TagStats;
import br.com.accessmap.backend.place.service.PlaceService;
import br.com.accessmap.backend.review.dto.ReviewRequestDto;
import br.com.accessmap.backend.review.dto.ReviewUpdateRequestDto;
import br.com.accessmap.backend.review.enums.AccessibilityTag;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.enums.SuspicionReason;
import br.com.accessmap.backend.review.enums.TrustLevel;
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

    public static final String LOCAL_JA_AVALIADO = "Você já avaliou este local. Edite sua avaliação existente.";

    private final ReviewRepository reviewRepository;
    private final PlaceService placeService;
    private final UserService userService;
    private final ReviewRateLimiter reviewRateLimiter;
    private final ReviewTrustCalculator reviewTrustCalculator;
    private final ReviewSuspicionRules suspicionRules;

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
            throw new ResponseStatusException(HttpStatus.CONFLICT, LOCAL_JA_AVALIADO);
        }

        if (reviewRepository.existsByUserIdAndPlaceIdAndStatus(
                userId, request.getPlaceId(), ReviewStatus.OCULTA)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Sua avaliação deste local está em análise."
            );
        }

        reviewRateLimiter.check(author);

        Place place = placeService.findOrCreateByPlaceId(request.getPlaceId());
        TrustLevel level = reviewTrustCalculator.levelOf(author);
        boolean outlier = suspicionRules.isRatingOutlier(
                level, request.getRating(), place.getReviewCount(), place.getAverageScore());

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
                .authorTrustLevel(level)
                .suspicionReason(outlier ? SuspicionReason.NOTA_DESTOANTE : null)
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

        flagIfEditedToOutlier(existing, request.getRating());

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
        if (!existing.getUserId().equals(userId)) {
            existing.setModerationRemovedAt(LocalDateTime.now());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(existing);

        recalculatePlaceAggregates(existing.getPlaceId());
    }

    /** Sem isto, uma conta nova publicaria uma nota comum e editaria depois para escapar da checagem. */
    private void flagIfEditedToOutlier(Review existing, Integer newRating) {
        boolean candidate = newRating != null && !newRating.equals(existing.getRating())
                && existing.getSuspicionReason() == null
                && existing.getAuthorTrustLevel() == TrustLevel.NOVA;
        if (!candidate) {
            return;
        }

        // Direto das somas: com peso, a média dos outros não dá para reconstruir a partir da média do local
        RatingTotals totals = ratingTotals(existing.getPlaceId());
        double weight = reviewTrustCalculator.weightOf(existing.getAuthorTrustLevel());
        RatingTotals others = new RatingTotals(totals.reviews() - 1,
                totals.weightedRatings() - existing.getRating() * weight, totals.weights() - weight);
        if (suspicionRules.isRatingOutlier(
                existing.getAuthorTrustLevel(), newRating, (int) others.reviews(), others.average())) {
            existing.setSuspicionReason(SuspicionReason.NOTA_DESTOANTE);
        }
    }

    /** Sai da nota do local e entra na fila do ADMIN. Dois envios simultâneos podem chegar aqui: a segunda chamada não faz nada. */
    @Transactional
    public void hideForReports(String id) {
        reviewRepository.findByIdAndStatus(id, ReviewStatus.PUBLICADA).ifPresent(review -> {
            review.setStatus(ReviewStatus.OCULTA);
            review.setSuspicionReason(SuspicionReason.DENUNCIAS);
            review.setUpdatedAt(LocalDateTime.now());
            reviewRepository.save(review);
            recalculatePlaceAggregates(review.getPlaceId());
        });
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
    public void recalculatePlaceAggregates(String placeId) {
        RatingTotals totals = ratingTotals(placeId);

        Map<AccessibilityTag, TagStats> tagStats = new EnumMap<>(AccessibilityTag.class);

        reviewRepository.tagStatsByPlaceId(placeId, ReviewStatus.PUBLICADA).forEach(row -> {
            TagStats stats = tagStats.computeIfAbsent(row.getTag(), tag -> TagStats.builder().build());
            switch (row.getAssessment()) {
                case ADEQUADO -> stats.setAdequadoCount(row.getTotal());
                case INADEQUADO -> stats.setInadequadoCount(row.getTotal());
                case INEXISTENTE -> stats.setInexistenteCount(row.getTotal());
            }
        });

        placeService.applyAggregates(placeId, totals.reviews(), totals.average(), tagStats);
    }

    /** Somas das reviews publicadas, com o peso do nível de cada uma (#27). */
    private RatingTotals ratingTotals(String placeId) {
        RatingTotals totals = new RatingTotals(0, 0.0, 0.0);
        for (ReviewRepository.LevelAggregate row
                : reviewRepository.aggregateByPlaceIdAndLevel(placeId, ReviewStatus.PUBLICADA)) {
            double weight = reviewTrustCalculator.weightOf(row.getLevel());
            totals = new RatingTotals(totals.reviews() + row.getReviewCount(),
                    totals.weightedRatings() + row.getRatingSum() * weight,
                    totals.weights() + row.getReviewCount() * weight);
        }
        return totals;
    }

    private record RatingTotals(long reviews, double weightedRatings, double weights) {

        double average() {
            return weights > 0 ? weightedRatings / weights : 0.0;
        }
    }
}
