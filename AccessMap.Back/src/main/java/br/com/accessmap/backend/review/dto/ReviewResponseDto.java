package br.com.accessmap.backend.review.dto;

import br.com.accessmap.backend.identity.enums.AccessibilityNeed;
import br.com.accessmap.backend.review.enums.AccessibilityTag;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.enums.TagAssessment;
import br.com.accessmap.backend.review.enums.TrustLevel;
import br.com.accessmap.backend.review.model.Review;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Avaliação como a API devolve. Sem {@code userId}: as rotas de leitura são públicas, e o identificador
 * de quem avaliou somado às necessidades declaradas é dado pessoal sensível (LGPD, art. 11).
 * {@code reviewerNeeds} só aparece para o próprio autor. {@code trustBadge} só vem para Nova e Confiável:
 * Regular não tem selo, e Restrita não sai da API para não expor uma punição.
 */
public record ReviewResponseDto(String id, String placeId, Integer rating, String comment,
                                Map<AccessibilityTag, TagAssessment> tags,
                                @JsonInclude(JsonInclude.Include.NON_NULL) Set<AccessibilityNeed> reviewerNeeds,
                                ReviewStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, boolean mine,
                                @JsonInclude(JsonInclude.Include.NON_NULL) TrustLevel trustBadge) {

    /** @param viewerId quem está vendo, extraído do token; {@code null} sem login */
    public static ReviewResponseDto from(Review review, String viewerId) {
        boolean mine = review.getUserId().equals(viewerId);
        return new ReviewResponseDto(review.getId(), review.getPlaceId(), review.getRating(), review.getComment(),
                review.getTags(), mine ? review.getReviewerNeeds() : null, review.getStatus(),
                review.getCreatedAt(), review.getUpdatedAt(), mine, badgeOf(review.getAuthorTrustLevel()));
    }

    private static TrustLevel badgeOf(TrustLevel level) {
        return level == TrustLevel.NOVA || level == TrustLevel.CONFIAVEL ? level : null;
    }
}
