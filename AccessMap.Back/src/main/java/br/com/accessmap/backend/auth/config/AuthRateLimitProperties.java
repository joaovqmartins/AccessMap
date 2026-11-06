package br.com.accessmap.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Quantas tentativas falhas são aceitas em {@code window} antes de responder 429. */
@ConfigurationProperties(prefix = "accessmap.auth.rate-limit")
public record AuthRateLimitProperties(Duration window, int loginPerPhone, int loginPerIp, int registerPerIp) {
}
