package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.identity.event.UserDeletedEvent;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Avaliações de quem exclui a conta continuam publicadas e contando na nota (decisão da #106), mas sem
 * ligação com a pessoa: o autor vira um id aleatório que não aponta para ninguém, e as necessidades
 * declaradas são apagadas. Comentário, nota, tags e nível gravado ficam.
 * <p>
 * Um id novo por conta, e não um texto fixo: o índice {@code uk_reviews_user_place_active} não deixa o
 * mesmo autor ter duas avaliações ativas no mesmo local, e contas excluídas diferentes colidiriam.
 */
@Component
@RequiredArgsConstructor
public class ReviewAnonymizer {

    private final ReviewRepository reviewRepository;

    @Transactional
    @EventListener
    public void onUserDeleted(UserDeletedEvent event) {
        reviewRepository.deleteReviewerNeedsOfUser(event.userId());
        reviewRepository.reassignUser(event.userId(), UUID.randomUUID().toString());
    }
}
