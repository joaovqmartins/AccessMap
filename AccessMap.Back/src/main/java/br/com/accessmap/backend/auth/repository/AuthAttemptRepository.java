package br.com.accessmap.backend.auth.repository;

import br.com.accessmap.backend.auth.enums.AuthAttemptKind;
import br.com.accessmap.backend.auth.model.AuthAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthAttemptRepository extends JpaRepository<AuthAttempt, String> {

    long countByKindAndKeyHashAndCreatedAtAfter(AuthAttemptKind kind, String keyHash, LocalDateTime since);

    Optional<AuthAttempt> findFirstByKindAndKeyHashAndCreatedAtAfterOrderByCreatedAtAsc(
            AuthAttemptKind kind, String keyHash, LocalDateTime since);

    void deleteByKindAndKeyHash(AuthAttemptKind kind, String keyHash);

    void deleteByCreatedAtBefore(LocalDateTime before);
}
