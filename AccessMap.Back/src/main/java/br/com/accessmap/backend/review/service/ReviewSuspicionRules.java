package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.review.config.ReviewRateLimitProperties;
import br.com.accessmap.backend.review.config.ReviewSuspicionProperties;
import br.com.accessmap.backend.review.enums.TrustLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Regras da #27 para sinalizar uma review. Só decide: quem aplica são os serviços de review e de denúncia. */
@Component
@RequiredArgsConstructor
public class ReviewSuspicionRules {

    private final ReviewSuspicionProperties suspicion;
    private final ReviewRateLimitProperties rateLimit;

    /** Conta fora do prazo de conta nova: só essas denúncias ocultam, o que dificulta criar contas para derrubar review. */
    public boolean isEstablished(User reporter) {
        return reporter.getCreatedAt() != null
                && !reporter.getCreatedAt().isAfter(LocalDateTime.now().minus(rateLimit.newAccountPeriod()));
    }

    public boolean reachedReportLimit(long establishedReports) {
        return establishedReports >= suspicion.reportsToHide();
    }

    /**
     * @param otherReviews quantas reviews o local tem sem contar esta
     * @param otherAverage média do local sem contar esta
     */
    public boolean isRatingOutlier(TrustLevel level, int rating, int otherReviews, double otherAverage) {
        return level == TrustLevel.NOVA
                && otherReviews >= suspicion.minPlaceReviews()
                && Math.abs(rating - otherAverage) >= suspicion.ratingGap();
    }
}
