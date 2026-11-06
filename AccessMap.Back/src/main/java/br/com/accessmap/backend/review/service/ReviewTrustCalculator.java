package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.review.config.ReviewRateLimitProperties;
import br.com.accessmap.backend.review.config.ReviewTrustProperties;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.enums.TrustLevel;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Nível de confiabilidade de uma conta (#27). Vale o primeiro que a conta cumprir: Restrita, Nova,
 * Confiável e, por fim, Regular. As necessidades de acessibilidade ficam fora: são dado sensível
 * e não dizem nada sobre honestidade.
 */
@Service
@RequiredArgsConstructor
public class ReviewTrustCalculator {

    private final ReviewRepository reviewRepository;
    private final ReviewTrustProperties trust;
    private final ReviewRateLimitProperties rateLimit;

    public TrustLevel levelOf(User author) {
        LocalDateTime now = LocalDateTime.now();
        long removed = reviewRepository.countByUserIdAndModerationRemovedAtAfter(
                author.getId(), now.minus(trust.moderationWindow()));

        if (removed >= trust.restrictedRemovals()) {
            return TrustLevel.RESTRITA;
        }

        LocalDateTime createdAt = author.getCreatedAt();
        if (createdAt == null) {
            return TrustLevel.REGULAR;
        }
        if (createdAt.isAfter(now.minus(rateLimit.newAccountPeriod()))) {
            return TrustLevel.NOVA;
        }

        boolean oldEnough = !createdAt.isAfter(now.minus(trust.trustedMinAge()));
        if (oldEnough && removed == 0 && published(author) >= trust.trustedMinPublished()) {
            return TrustLevel.CONFIAVEL;
        }
        return TrustLevel.REGULAR;
    }

    /** Peso da avaliação na média do local. Sai da configuração, para o grupo poder trocar sem migrar dados. */
    public double weightOf(TrustLevel level) {
        return switch (level) {
            case RESTRITA -> trust.weightRestricted();
            case NOVA -> trust.weightNew();
            case REGULAR -> trust.weightRegular();
            case CONFIAVEL -> trust.weightTrusted();
        };
    }

    private long published(User author) {
        return reviewRepository.countByUserIdAndStatus(author.getId(), ReviewStatus.PUBLICADA);
    }
}
