package br.com.accessmap.backend.review.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Quantas avaliações um usuário pode criar por janela de tempo. Conta nova (criada há menos de
 * {@code newAccountPeriod}) tem limite menor. Os valores são provisórios até as regras da #27.
 */
@ConfigurationProperties(prefix = "accessmap.review.rate-limit")
public record ReviewRateLimitProperties(int perHour, int perDay, int newAccountPerHour, int newAccountPerDay,
                                        Duration newAccountPeriod) {
}
