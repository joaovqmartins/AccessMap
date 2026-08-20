package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.service.UserService;
import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.service.PlaceService;
import br.com.accessmap.backend.review.dto.ReviewRequestDto;
import br.com.accessmap.backend.review.enums.AccessibilityTag;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PlaceService placeService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReviewService reviewService;

    private ReviewRequestDto validRequest() {
        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setUserId("user-1");
        dto.setPlaceId("place-1");
        dto.setRating(4);
        dto.setComment("Rampa de acesso boa, banheiro adaptado.");
        dto.setTags(Set.of(AccessibilityTag.RAMPAS_E_ENTRADAS, AccessibilityTag.BANHEIROS_ADAPTADOS));
        return dto;
    }

    @Test
    void deveCriarReviewValidandoUsuarioEAtualizandoPlace() {
        when(userService.findById("user-1")).thenReturn(User.builder().id("user-1").build());
        when(placeService.findOrCreateByPlaceId("place-1")).thenReturn(Place.builder().placeId("place-1").build());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Review review = reviewService.create(validRequest());

        assertThat(review.getUserId()).isEqualTo("user-1");
        assertThat(review.getPlaceId()).isEqualTo("place-1");
        assertThat(review.getRating()).isEqualTo(4);
        verify(placeService).registerNewReview("place-1", 4);
    }

    @Test
    void deveRejeitarReviewParaUsuarioInexistente() {
        when(userService.findById("user-invalido")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        ReviewRequestDto dto = validRequest();
        dto.setUserId("user-invalido");

        assertThrows(ResponseStatusException.class, () -> reviewService.create(dto));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deveLancarNotFoundQuandoReviewNaoExiste() {
        when(reviewRepository.findById("id-invalido")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> reviewService.findById("id-invalido"));
    }

    @Test
    void deveListarReviewsPorPlaceId() {
        Review review = Review.builder().placeId("place-1").build();
        when(reviewRepository.findByPlaceId("place-1")).thenReturn(List.of(review));

        List<Review> resultado = reviewService.findByPlaceId("place-1");

        assertThat(resultado).containsExactly(review);
    }
}
