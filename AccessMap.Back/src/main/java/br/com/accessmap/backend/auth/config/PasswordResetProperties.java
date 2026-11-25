package br.com.accessmap.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Recuperação de senha por e-mail (#109).
 *
 * @param codeTtl     validade do código
 * @param maxAttempts tentativas erradas que invalidam o código; com 6 dígitos, 5 chances dão 1 em 200 mil
 */
@ConfigurationProperties(prefix = "accessmap.auth.password-reset")
public record PasswordResetProperties(Duration codeTtl, int maxAttempts) {
}
