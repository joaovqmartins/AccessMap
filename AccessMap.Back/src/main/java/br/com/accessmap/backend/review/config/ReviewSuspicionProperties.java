package br.com.accessmap.backend.review.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Sinais de review suspeita definidos na #27. */
@ConfigurationProperties(prefix = "accessmap.review.suspicion")
public record ReviewSuspicionProperties(int reportsToHide, int ratingGap, int minPlaceReviews) {
}
