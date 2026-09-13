package br.com.accessmap.backend.auth.service;

import br.com.accessmap.backend.auth.config.JwtProperties;
import br.com.accessmap.backend.auth.model.RefreshToken;
import br.com.accessmap.backend.auth.repository.RefreshTokenRepository;
import br.com.accessmap.backend.identity.event.PasswordChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Slf4j
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final JwtProperties props;
    private final TransactionTemplate requiresNew;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               TokenService tokenService,
                               JwtProperties props,
                               PlatformTransactionManager transactionManager) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.props = props;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Gera um refresh token novo para o usuário e persiste apenas o hash. Retorna o valor em claro. */
    @Transactional
    public String issue(String userId) {
        String raw = tokenService.generateRefreshToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(tokenService.hash(raw))
                .userId(userId)
                .expiresAt(LocalDateTime.now().plus(props.refreshTokenTtl()))
                .createdAt(LocalDateTime.now())
                .build());
        return raw;
    }

    /**
     * Valida e consome (revoga) o refresh token apresentado, devolvendo o userId dono.
     * Um token já revogado sendo reapresentado indica roubo: todas as sessões do usuário são derrubadas.
     */
    @Transactional
    public String rotate(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenService.hash(rawToken))
                .orElseThrow(this::invalidSession);

        if (stored.isRevoked()) {
            log.warn("Reuso de refresh token revogado detectado para o usuário {}; revogando todas as sessões",
                    stored.getUserId());
            // Em transação própria: a revogação precisa persistir mesmo com o 401 que
            // encerra a requisição fazendo rollback da transação corrente.
            requiresNew.executeWithoutResult(tx -> revokeAll(stored.getUserId()));
            throw invalidSession();
        }
        if (stored.isExpired()) {
            throw invalidSession();
        }

        stored.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(stored);
        return stored.getUserId();
    }

    /** Revoga o token apresentado, se existir e ainda estiver ativo. Nunca falha: logout é idempotente. */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(tokenService.hash(rawToken))
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> {
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void revokeAll(String userId) {
        refreshTokenRepository.revokeAllActiveByUserId(userId, LocalDateTime.now());
    }

    @EventListener
    public void onPasswordChanged(PasswordChangedEvent event) {
        revokeAll(event.userId());
    }

    private ResponseStatusException invalidSession() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida. Faça login novamente.");
    }
}
