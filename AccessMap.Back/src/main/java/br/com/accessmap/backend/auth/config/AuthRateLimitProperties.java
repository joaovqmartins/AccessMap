package br.com.accessmap.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Quantas tentativas são aceitas em {@code window} antes de responder 429. No login e no cadastro
 * contam só as falhas; no pedido de código de recuperação de senha conta todo pedido.
 */
@ConfigurationProperties(prefix = "accessmap.auth.rate-limit")
public record AuthRateLimitProperties(Duration window, int loginPerPhone, int loginPerIp, int registerPerIp,
                                      int resetPerIp, int resetPerEmail) {
}
