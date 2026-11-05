package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.review.config.ReviewRateLimitProperties;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Camada preventiva contra manipulação de nota: barra o volume antes de ele chegar à média.
 * Dois envios exatamente simultâneos podem passar juntos pela contagem; para este caso não vale travar o banco.
 */
@Service
@RequiredArgsConstructor
public class ReviewRateLimiter {

    private final ReviewRepository reviewRepository;
    private final ReviewRateLimitProperties limits;

    /** Lança 429 se {@code author} já atingiu o limite. ADMIN não tem limite. */
    public void check(User author) {
        if (author.getRole() == Role.ADMIN) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean newAccount = author.getCreatedAt() != null
                && author.getCreatedAt().isAfter(now.minus(limits.newAccountPeriod()));
        int perHour = newAccount ? limits.newAccountPerHour() : limits.perHour();
        int perDay = newAccount ? limits.newAccountPerDay() : limits.perDay();

        if (createdSince(author, now.minusHours(1)) >= perHour || createdSince(author, now.minusDays(1)) >= perDay) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Você publicou muitas avaliações em pouco tempo. Tente novamente mais tarde."
            );
        }
    }

    private long createdSince(User author, LocalDateTime since) {
        return reviewRepository.countByUserIdAndCreatedAtAfter(author.getId(), since);
    }
}
