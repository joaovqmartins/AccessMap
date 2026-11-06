package br.com.accessmap.backend.review.dto;

import br.com.accessmap.backend.review.enums.AccessibilityTag;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.enums.SuspicionReason;
import br.com.accessmap.backend.review.enums.TagAssessment;
import br.com.accessmap.backend.review.enums.TrustLevel;
import br.com.accessmap.backend.review.model.Review;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Review como a moderação vê: com o autor, que a resposta pública esconde. Sem {@code reviewerNeeds}:
 * é dado sensível e não ajuda a julgar a review.
 */
public record AdminReviewDto(String id, String placeId, String userId, Integer rating, String comment,
                             Map<AccessibilityTag, TagAssessment> tags, ReviewStatus status,
                             SuspicionReason suspicionReason, long reportCount, TrustLevel authorTrustLevel,
                             LocalDateTime createdAt) {

    public static AdminReviewDto from(Review review, long reportCount) {
        return new AdminReviewDto(review.getId(), review.getPlaceId(), review.getUserId(), review.getRating(),
                review.getComment(), review.getTags(), review.getStatus(), review.getSuspicionReason(), reportCount,
                review.getAuthorTrustLevel(), review.getCreatedAt());
    }
}
