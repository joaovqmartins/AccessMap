package br.com.accessmap.backend.auth.service;

import br.com.accessmap.backend.auth.config.AuthRateLimitProperties;
import br.com.accessmap.backend.auth.enums.AuthAttemptKind;
import br.com.accessmap.backend.auth.model.AuthAttempt;
import br.com.accessmap.backend.auth.repository.AuthAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Limita tentativas falhas de login e cadastro. Conta qualquer telefone digitado, exista ou não,
 * para que o 429 também não revele quem tem conta.
 */
@Service
@RequiredArgsConstructor
public class AuthAttemptService {

    private final AuthAttemptRepository authAttemptRepository;
    private final AuthRateLimitProperties limits;

    /** Vem antes de conferir a senha: senão quem está bloqueado continuaria testando senhas. */
    public void assertLoginAllowed(String phone, String ip) {
        assertBelowLimit(AuthAttemptKind.LOGIN_PHONE, phone, limits.loginPerPhone());
        assertBelowLimit(AuthAttemptKind.LOGIN_IP, ip, limits.loginPerIp());
    }

    public void assertRegisterAllowed(String ip) {
        assertBelowLimit(AuthAttemptKind.REGISTER_IP, ip, limits.registerPerIp());
    }

    /** Transação própria: a exceção que recusa o login desfaz a transação dele, e a falha se perderia junto. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFailure(String phone, String ip) {
        record(AuthAttemptKind.LOGIN_PHONE, phone);
        record(AuthAttemptKind.LOGIN_IP, ip);
        purgeExpired();
    }

    /** Transação própria pelo mesmo motivo de {@link #recordLoginFailure}. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRegisterFailure(String ip) {
        record(AuthAttemptKind.REGISTER_IP, ip);
        purgeExpired();
    }

    /** Login correto zera o telefone. O IP não: quem testa vários telefones acerta um e continuaria testando. */
    @Transactional
    public void clearLoginFailures(String phone) {
        authAttemptRepository.deleteByKindAndKeyHash(AuthAttemptKind.LOGIN_PHONE, hash(phone));
    }

    private void assertBelowLimit(AuthAttemptKind kind, String key, int limit) {
        String keyHash = hash(key);
        LocalDateTime since = LocalDateTime.now().minus(limits.window());

        if (authAttemptRepository.countByKindAndKeyHashAndCreatedAtAfter(kind, keyHash, since) < limit) {
            return;
        }

        long minutes = authAttemptRepository
                .findFirstByKindAndKeyHashAndCreatedAtAfterOrderByCreatedAtAsc(kind, keyHash, since)
                .map(oldest -> minutesUntilExpired(oldest.getCreatedAt()))
                .orElse(1L);
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Muitas tentativas. Tente novamente em " + minutes + (minutes == 1 ? " minuto." : " minutos."));
    }

    /** Arredonda para cima: dizer "0 minutos" ou liberar depois do prometido confunde. */
    private long minutesUntilExpired(LocalDateTime attemptAt) {
        Duration remaining = Duration.between(LocalDateTime.now(), attemptAt.plus(limits.window()));
        return Math.max(1, (remaining.toSeconds() + 59) / 60);
    }

    private void record(AuthAttemptKind kind, String key) {
        authAttemptRepository.save(AuthAttempt.builder()
                .kind(kind)
                .keyHash(hash(key))
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void purgeExpired() {
        authAttemptRepository.deleteByCreatedAtBefore(LocalDateTime.now().minus(limits.window()));
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
