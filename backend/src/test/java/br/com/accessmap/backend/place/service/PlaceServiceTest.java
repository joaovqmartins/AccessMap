package br.com.accessmap.backend.place.service;

import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.repository.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private PlaceService placeService;

    @Test
    void deveLancarNotFoundQuandoPlaceIdNaoExiste() {
        when(placeRepository.findByPlaceId("abc")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> placeService.findByPlaceId("abc"));
    }

    @Test
    void deveCriarPlaceQuandoNaoExisteAoBuscarOuCriar() {
        when(placeRepository.findByPlaceId("abc")).thenReturn(Optional.empty());
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Place place = placeService.findOrCreateByPlaceId("abc");

        assertThat(place.getPlaceId()).isEqualTo("abc");
        assertThat(place.getAverageScore()).isEqualTo(0.0);
        assertThat(place.getReviewCount()).isEqualTo(0);
        verify(placeRepository).save(any(Place.class));
    }

    @Test
    void naoDeveCriarPlaceDuplicadoQuandoJaExiste() {
        Place existente = Place.builder().placeId("abc").averageScore(4.0).reviewCount(2).build();
        when(placeRepository.findByPlaceId("abc")).thenReturn(Optional.of(existente));

        Place resultado = placeService.findOrCreateByPlaceId("abc");

        assertThat(resultado.getReviewCount()).isEqualTo(2);
        verify(placeRepository, never()).save(any());
    }

    @Test
    void deveDefinirScoreIgualAoRatingNaPrimeiraReview() {
        Place novo = Place.builder().placeId("abc").averageScore(0.0).reviewCount(0).build();
        when(placeRepository.findByPlaceId("abc")).thenReturn(Optional.of(novo));
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));

        placeService.registerNewReview("abc", 5);

        assertThat(novo.getReviewCount()).isEqualTo(1);
        assertThat(novo.getAverageScore()).isEqualTo(5.0);
    }

    @Test
    void deveRecalcularMediaPonderadaAoRegistrarSegundaReview() {
        Place existente = Place.builder().placeId("abc").averageScore(5.0).reviewCount(1).build();
        when(placeRepository.findByPlaceId("abc")).thenReturn(Optional.of(existente));
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));

        placeService.registerNewReview("abc", 3);

        assertThat(existente.getReviewCount()).isEqualTo(2);
        assertThat(existente.getAverageScore()).isEqualTo(4.0);
    }
}
