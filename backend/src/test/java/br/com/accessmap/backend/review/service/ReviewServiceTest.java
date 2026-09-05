package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.identity.enums.AccessibilityNeed;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.service.UserService;
import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.model.TagStats;
import br.com.accessmap.backend.place.service.PlaceService;
import br.com.accessmap.backend.review.dto.ReviewRequestDto;
import br.com.accessmap.backend.review.dto.ReviewUpdateRequestDto;
import br.com.accessmap.backend.review.enums.AccessibilityTag;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.enums.TagAssessment;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
        dto.setTags(Map.of(
                AccessibilityTag.RAMPAS_E_ENTRADAS, TagAssessment.ADEQUADO,
                AccessibilityTag.BANHEIROS_ADAPTADOS, TagAssessment.ADEQUADO
        ));
        return dto;
    }

    private Review reviewPublicada() {
        return Review.builder()
                .id("review-1")
                .userId("user-1")
                .placeId("place-1")
                .rating(4)
                .comment("Comentario original")
                .tags(Map.of(AccessibilityTag.RAMPAS_E_ENTRADAS, TagAssessment.ADEQUADO))
                .status(ReviewStatus.PUBLICADA)
                .build();
    }

    // Evita repetir o stub das duas consultas de recomputacao em cada teste de escrita.
    private void stubRecalculo(long reviewCount, long ratingSum) {
        ReviewRepository.PlaceAggregate aggregate = new ReviewRepository.PlaceAggregate() {
            @Override
            public long getReviewCount() {
                return reviewCount;
            }

            @Override
            public long getRatingSum() {
                return ratingSum;
            }
        };
        lenient().when(reviewRepository.aggregateByPlaceId(anyString(), eq(ReviewStatus.PUBLICADA)))
                .thenReturn(aggregate);
        lenient().when(reviewRepository.tagStatsByPlaceId(anyString(), eq(ReviewStatus.PUBLICADA)))
                .thenReturn(List.of());
    }

    private ReviewRepository.TagAssessmentCount tagCount(AccessibilityTag tag, TagAssessment assessment, long total) {
        return new ReviewRepository.TagAssessmentCount() {
            @Override
            public AccessibilityTag getTag() {
                return tag;
            }

            @Override
            public TagAssessment getAssessment() {
                return assessment;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    @Test
    void deveCriarReviewValidandoUsuarioEAtualizandoPlace() {
        when(userService.findById("user-1")).thenReturn(User.builder().id("user-1").build());
        when(placeService.findOrCreateByPlaceId("place-1")).thenReturn(Place.builder().placeId("place-1").build());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubRecalculo(1, 4);

        Review review = reviewService.create(validRequest());

        assertThat(review.getUserId()).isEqualTo("user-1");
        assertThat(review.getPlaceId()).isEqualTo("place-1");
        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLICADA);
        verify(placeService).applyAggregates(eq("place-1"), eq(1L), eq(4L), any());
    }

    @Test
    void deveCopiarNecessidadesDoAutorParaAReview() {
        User autor = User.builder()
                .id("user-1")
                .accessibilityNeeds(Set.of(AccessibilityNeed.DEFICIENCIA_VISUAL))
                .build();
        when(userService.findById("user-1")).thenReturn(autor);
        when(placeService.findOrCreateByPlaceId("place-1")).thenReturn(Place.builder().placeId("place-1").build());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubRecalculo(1, 4);

        Review review = reviewService.create(validRequest());

        assertThat(review.getReviewerNeeds()).containsExactly(AccessibilityNeed.DEFICIENCIA_VISUAL);
        assertThat(review.getReviewerNeeds()).isNotSameAs(autor.getAccessibilityNeeds());
    }

    @Test
    void deveRejeitarSegundaReviewDoMesmoUsuarioNoMesmoLocal() {
        when(userService.findById("user-1")).thenReturn(User.builder().id("user-1").build());
        when(reviewRepository.existsByUserIdAndPlaceIdAndStatus("user-1", "place-1", ReviewStatus.PUBLICADA))
                .thenReturn(true);

        ResponseStatusException erro =
                assertThrows(ResponseStatusException.class, () -> reviewService.create(validRequest()));

        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(reviewRepository, never()).save(any());
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
        when(reviewRepository.findByIdAndStatus("id-invalido", ReviewStatus.PUBLICADA))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> reviewService.findById("id-invalido"));
    }

    @Test
    void deveListarApenasReviewsPublicadasDoLocal() {
        Review review = reviewPublicada();
        when(reviewRepository.findByPlaceIdAndStatus("place-1", ReviewStatus.PUBLICADA))
                .thenReturn(List.of(review));

        assertThat(reviewService.findByPlaceId("place-1")).containsExactly(review);
    }

    @Test
    void deveListarReviewsDoUsuario() {
        Review review = reviewPublicada();
        when(reviewRepository.findByUserIdAndStatus("user-1", ReviewStatus.PUBLICADA))
                .thenReturn(List.of(review));

        assertThat(reviewService.findByUserId("user-1")).containsExactly(review);
    }

    @Test
    void deveAtualizarRatingERecalcularAgregado() {
        Review existente = reviewPublicada();
        when(reviewRepository.findByIdAndStatus("review-1", ReviewStatus.PUBLICADA))
                .thenReturn(Optional.of(existente));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubRecalculo(1, 2);

        ReviewUpdateRequestDto dto = new ReviewUpdateRequestDto();
        dto.setRating(2);

        Review atualizado = reviewService.update("review-1", dto);

        assertThat(atualizado.getRating()).isEqualTo(2);
        assertThat(atualizado.getComment()).isEqualTo("Comentario original");
        assertThat(atualizado.getUpdatedAt()).isNotNull();
        verify(placeService).applyAggregates(eq("place-1"), eq(1L), eq(2L), any());
    }

    @Test
    void deveAtualizarApenasCamposInformados() {
        Review existente = reviewPublicada();
        when(reviewRepository.findByIdAndStatus("review-1", ReviewStatus.PUBLICADA))
                .thenReturn(Optional.of(existente));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubRecalculo(1, 4);

        ReviewUpdateRequestDto dto = new ReviewUpdateRequestDto();
        dto.setComment("Comentario novo");
        dto.setTags(Map.of(AccessibilityTag.ELEVADORES, TagAssessment.INEXISTENTE));

        Review atualizado = reviewService.update("review-1", dto);

        assertThat(atualizado.getComment()).isEqualTo("Comentario novo");
        assertThat(atualizado.getTags())
                .containsExactly(Map.entry(AccessibilityTag.ELEVADORES, TagAssessment.INEXISTENTE));
        assertThat(atualizado.getRating()).isEqualTo(4);
    }

    @Test
    void deveRejeitarAtualizacaoComMapaDeTagsVazio() {
        Review existente = reviewPublicada();
        when(reviewRepository.findByIdAndStatus("review-1", ReviewStatus.PUBLICADA))
                .thenReturn(Optional.of(existente));

        ReviewUpdateRequestDto dto = new ReviewUpdateRequestDto();
        dto.setTags(Map.of());

        assertThrows(ResponseStatusException.class, () -> reviewService.update("review-1", dto));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deveLancarNotFoundAoAtualizarReviewInexistente() {
        when(reviewRepository.findByIdAndStatus("id-invalido", ReviewStatus.PUBLICADA))
                .thenReturn(Optional.empty());

        ReviewUpdateRequestDto dto = new ReviewUpdateRequestDto();
        dto.setRating(3);

        assertThrows(ResponseStatusException.class, () -> reviewService.update("id-invalido", dto));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deveMarcarReviewComoRemovidaEmVezDeApagar() {
        Review existente = reviewPublicada();
        when(reviewRepository.findByIdAndStatus("review-1", ReviewStatus.PUBLICADA))
                .thenReturn(Optional.of(existente));
        stubRecalculo(0, 0);

        reviewService.delete("review-1");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReviewStatus.REMOVIDA);
        verify(reviewRepository, never()).deleteById(anyString());
        verify(placeService).applyAggregates(eq("place-1"), eq(0L), eq(0L), any());
    }

    @Test
    void deveLancarNotFoundAoRemoverReviewJaRemovida() {
        when(reviewRepository.findByIdAndStatus("review-1", ReviewStatus.PUBLICADA))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> reviewService.delete("review-1"));
        verify(reviewRepository, never()).save(any());
        verify(placeService, never()).applyAggregates(anyString(), anyLong(), anyLong(), any());
    }

    @Test
    void deveAgruparContagemPorTagAoRecalcular() {
        Review existente = reviewPublicada();
        when(reviewRepository.findByIdAndStatus("review-1", ReviewStatus.PUBLICADA))
                .thenReturn(Optional.of(existente));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubRecalculo(3, 11);
        when(reviewRepository.tagStatsByPlaceId("place-1", ReviewStatus.PUBLICADA)).thenReturn(List.of(
                tagCount(AccessibilityTag.RAMPAS_E_ENTRADAS, TagAssessment.ADEQUADO, 2L),
                tagCount(AccessibilityTag.RAMPAS_E_ENTRADAS, TagAssessment.INADEQUADO, 1L),
                tagCount(AccessibilityTag.ELEVADORES, TagAssessment.INEXISTENTE, 3L)
        ));

        ReviewUpdateRequestDto dto = new ReviewUpdateRequestDto();
        dto.setRating(5);
        reviewService.update("review-1", dto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<AccessibilityTag, TagStats>> captor = ArgumentCaptor.forClass(Map.class);
        verify(placeService).applyAggregates(eq("place-1"), eq(3L), eq(11L), captor.capture());

        Map<AccessibilityTag, TagStats> stats = captor.getValue();
        assertThat(stats.get(AccessibilityTag.RAMPAS_E_ENTRADAS).getAdequadoCount()).isEqualTo(2L);
        assertThat(stats.get(AccessibilityTag.RAMPAS_E_ENTRADAS).getInadequadoCount()).isEqualTo(1L);
        assertThat(stats.get(AccessibilityTag.RAMPAS_E_ENTRADAS).getInexistenteCount()).isEqualTo(0L);
        assertThat(stats.get(AccessibilityTag.ELEVADORES).getInexistenteCount()).isEqualTo(3L);
    }
}
