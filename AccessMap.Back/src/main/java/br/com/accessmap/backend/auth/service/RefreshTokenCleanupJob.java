package br.com.accessmap.backend.auth.service;

import br.com.accessmap.backend.auth.config.RefreshTokenCleanupProperties;
import br.com.accessmap.backend.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Apaga os refresh tokens expirados e os revogados há mais que a retenção. Os revogados recentes
 * ficam no banco: é por eles que {@link RefreshTokenService#rotate} detecta o reuso de um token.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCleanupProperties props;

    @Scheduled(cron = "${accessmap.auth.refresh-cleanup.cron}")
    @Transactional
    public void cleanUp() {
        LocalDateTime now = LocalDateTime.now();
        int removed = refreshTokenRepository.deleteExpiredOrRevokedBefore(now, now.minus(props.revokedRetention()));
        log.info("Limpeza de refresh tokens: {} removidos (expirados ou revogados há mais de {})",
                removed, props.revokedRetention());
    }
}
