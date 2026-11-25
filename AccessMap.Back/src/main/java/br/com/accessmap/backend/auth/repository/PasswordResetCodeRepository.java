package br.com.accessmap.backend.auth.repository;

import br.com.accessmap.backend.auth.model.PasswordResetCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, String> {

    /**
     * O código que ainda vale: não usado e dentro da validade. Pedir outro invalida os anteriores.
     * Trava a linha: duas confirmações ao mesmo tempo não podem ganhar tentativas a mais.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM PasswordResetCode c
            WHERE c.userId = :userId AND c.usedAt IS NULL AND c.expiresAt > :now
            ORDER BY c.createdAt DESC LIMIT 1
            """)
    Optional<PasswordResetCode> findActive(String userId, LocalDateTime now);

    /** Marca como usados os códigos em aberto: só o último pedido vale. */
    @Modifying
    @Query("UPDATE PasswordResetCode c SET c.usedAt = :now WHERE c.userId = :userId AND c.usedAt IS NULL")
    int invalidateActiveByUserId(String userId, LocalDateTime now);

    @Modifying
    @Query("DELETE FROM PasswordResetCode c WHERE c.expiresAt < :now OR c.usedAt IS NOT NULL")
    int deleteExpiredOrUsed(LocalDateTime now);
}
