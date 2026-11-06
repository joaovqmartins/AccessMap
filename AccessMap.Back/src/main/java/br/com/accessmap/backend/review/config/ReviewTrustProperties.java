package br.com.accessmap.backend.review.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Regras dos níveis de confiabilidade definidas na #27. O prazo de conta nova é o mesmo da #100. */
@ConfigurationProperties(prefix = "accessmap.review.trust")
public record ReviewTrustProperties(Duration trustedMinAge, int trustedMinPublished, Duration moderationWindow,
                                    int restrictedRemovals, double weightRestricted, double weightNew,
                                    double weightRegular, double weightTrusted) {
}
