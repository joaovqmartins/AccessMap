package br.com.accessmap.backend.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Limpeza dos refresh tokens: quando roda ({@code cron}) e por quanto tempo um token revogado
 * é mantido ({@code revokedRetention}) antes de ser apagado. A retenção precisa cobrir a janela
 * em que um reuso ainda deve ser detectado.
 */
@Validated
@ConfigurationProperties(prefix = "accessmap.auth.refresh-cleanup")
public record RefreshTokenCleanupProperties(

        @NotBlank String cron,

        @NotNull Duration revokedRetention
) {
}
