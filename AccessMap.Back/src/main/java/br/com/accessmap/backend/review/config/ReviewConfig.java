package br.com.accessmap.backend.review.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ReviewRateLimitProperties.class, ReviewTrustProperties.class})
public class ReviewConfig {
}
