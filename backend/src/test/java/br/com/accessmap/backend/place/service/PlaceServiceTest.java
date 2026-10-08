package br.com.accessmap.backend.place.service;

import br.com.accessmap.backend.place.dto.PlaceSummaryDto;
import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.model.TagStats;
import br.com.accessmap.backend.place.repository.PlaceRepository;
import br.com.accessmap.backend.review.enums.AccessibilityTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private PlaceService placeService;

    private Map<AccessibilityTag, TagStats> semTags() {
        return new EnumMap<>(AccessibilityTag.class);
    }

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
    void deveDerivarMediaDaSomaEDaContagem() {
        Place existente = Place.builder().placeId("abc").build();
        when(placeRepository.findByPlaceId("abc")).thenReturn(Optional.of(existente));
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));

        placeService.applyAggregates("abc", 4, 14, semTags());

        assertThat(existente.getReviewCount()).isEqualTo(4);
        assertThat(existente.getAverageScore()).isEqualTo(3.5);
        assertThat(existente.getUpdatedAt()).isNotNull();
    }

    @Test
    void deveZerarAgregadoQuandoNaoHaReviewPublicada() {
        Place existente = Place.builder().placeId("abc").averageScore(4.5).reviewCount(6).build();
        when(placeRepository.findByPlaceId("abc")).thenReturn(Optional.of(existente));
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));

        placeService.applyAggregates("abc", 0, 0, semTags());

        assertThat(existente.getReviewCount()).isEqualTo(0);
        assertThat(existente.getAverageScore()).isEqualTo(0.0);
    }

    @Test
    void deveSubstituirEstatisticasPorTag() {
        Place existente = Place.builder().placeId("abc").build();
        existente.getTagStats().put(AccessibilityTag.ELEVADORES, TagStats.builder().adequadoCount(9L).build());
        when(placeRepository.findByPlaceId("abc")).thenReturn(Optional.of(existente));
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<AccessibilityTag, TagStats> novas = semTags();
        novas.put(AccessibilityTag.RAMPAS_E_ENTRADAS, TagStats.builder().adequadoCount(2L).inadequadoCount(1L).build());

        placeService.applyAggregates("abc", 3, 12, novas);

        assertThat(existente.getTagStats()).containsOnlyKeys(AccessibilityTag.RAMPAS_E_ENTRADAS);
        assertThat(existente.getTagStats().get(AccessibilityTag.RAMPAS_E_ENTRADAS).getAdequadoCount()).isEqualTo(2L);
        assertThat(existente.getTagStats().get(AccessibilityTag.RAMPAS_E_ENTRADAS).getInadequadoCount()).isEqualTo(1L);
    }

    @Test
    void deveCriarPlaceAoAplicarAgregadosDeLocalAindaNaoRegistrado() {
        when(placeRepository.findByPlaceId("abc")).thenReturn(Optional.empty());
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> invocation.getArgument(0));

        placeService.applyAggregates("abc", 1, 5, semTags());

        verify(placeRepository, never()).findByPlaceId("outro");
        verify(placeRepository, org.mockito.Mockito.times(2)).save(any(Place.class));
    }

    // ---------- findBatch ----------

    private Place local(String placeId, double media, int total) {
        return Place.builder().placeId(placeId).averageScore(media).reviewCount(total).build();
    }

    @Test
    void findBatchDeveManterAOrdemDaEntradaMesmoQueORepositorioDevolvaEmOutra() {
        when(placeRepository.findByPlaceIdIn(List.of("c", "a", "b")))
                .thenReturn(List.of(local("a", 4.0, 2), local("b", 3.0, 1), local("c", 5.0, 3)));

        List<PlaceSummaryDto> resultado = placeService.findBatch(List.of("c", "a", "b"), null);

        assertThat(resultado).extracting(PlaceSummaryDto::placeId).containsExactly("c", "a", "b");
        assertThat(resultado.get(0).averageScore()).isEqualTo(5.0);
        assertThat(resultado.get(0).reviewCount()).isEqualTo(3);
    }

    @Test
    void findBatchDeveDevolverZeradoOLocalQueNinguemAvaliouMantendoAPosicao() {
        when(placeRepository.findByPlaceIdIn(List.of("novo", "a")))
                .thenReturn(List.of(local("a", 4.0, 2)));

        List<PlaceSummaryDto> resultado = placeService.findBatch(List.of("novo", "a"), Set.of());

        assertThat(resultado).extracting(PlaceSummaryDto::placeId).containsExactly("novo", "a");
        assertThat(resultado.get(0).averageScore()).isEqualTo(0.0);
        assertThat(resultado.get(0).reviewCount()).isEqualTo(0);
        assertThat(resultado.get(0).tagStats()).isEmpty();
    }

    @Test
    void findBatchDeveRemoverIdsDuplicadosAntesDeConsultar() {
        when(placeRepository.findByPlaceIdIn(List.of("a", "b")))
                .thenReturn(List.of(local("a", 4.0, 2), local("b", 3.0, 1)));

        List<PlaceSummaryDto> resultado = placeService.findBatch(List.of("a", "b", "a"), null);

        assertThat(resultado).extracting(PlaceSummaryDto::placeId).containsExactly("a", "b");
        verify(placeRepository).findByPlaceIdIn(List.of("a", "b"));
    }

    @Test
    void findBatchSemTagsNaoDeveUsarAConsultaComFiltro() {
        when(placeRepository.findByPlaceIdIn(List.of("a"))).thenReturn(List.of());

        placeService.findBatch(List.of("a"), null);
        placeService.findBatch(List.of("a"), Set.of());

        verify(placeRepository, org.mockito.Mockito.times(2)).findByPlaceIdIn(List.of("a"));
        verify(placeRepository, never()).findByPlaceIdInAndAllTagsAdequate(any(), any(), anyLong());
    }

    @Test
    void findBatchComTagsDeveUsarAConsultaComFiltroEOTotalDeTags() {
        Set<AccessibilityTag> tags = Set.of(AccessibilityTag.RAMPAS_E_ENTRADAS, AccessibilityTag.ELEVADORES);
        when(placeRepository.findByPlaceIdInAndAllTagsAdequate(List.of("a"), tags, 2L))
                .thenReturn(List.of(local("a", 4.0, 2)));

        List<PlaceSummaryDto> resultado = placeService.findBatch(List.of("a"), tags);

        assertThat(resultado).extracting(PlaceSummaryDto::placeId).containsExactly("a");
        verify(placeRepository, never()).findByPlaceIdIn(any());
    }

    @Test
    void findBatchComTagsDeveDescartarLocalDesconhecidoOuQueNaoPassouNoFiltro() {
        Set<AccessibilityTag> tags = Set.of(AccessibilityTag.RAMPAS_E_ENTRADAS);
        when(placeRepository.findByPlaceIdInAndAllTagsAdequate(List.of("passa", "reprova", "novo"), tags, 1L))
                .thenReturn(List.of(local("passa", 5.0, 1)));

        List<PlaceSummaryDto> resultado = placeService.findBatch(List.of("passa", "reprova", "novo"), tags);

        assertThat(resultado).extracting(PlaceSummaryDto::placeId).containsExactly("passa");
    }

    // ---------- search ----------

    @Test
    void searchSemTagsDeveListarTudoSemUsarAConsultaComFiltro() {
        Pageable pagina = PageRequest.of(0, 20);
        Page<Place> todos = new PageImpl<>(List.of(local("a", 4.0, 2)));
        when(placeRepository.findAll(pagina)).thenReturn(todos);

        assertThat(placeService.search(null, pagina)).isSameAs(todos);
        assertThat(placeService.search(Set.of(), pagina)).isSameAs(todos);

        verify(placeRepository, never()).findByAllTagsAdequate(any(), anyLong(), any());
    }

    @Test
    void searchComVariasTagsDeveRepassarOConjuntoEOTotalPedido() {
        Pageable pagina = PageRequest.of(0, 20);
        Set<AccessibilityTag> tags = Set.of(AccessibilityTag.RAMPAS_E_ENTRADAS, AccessibilityTag.ELEVADORES);
        Page<Place> filtrados = new PageImpl<>(List.of(local("a", 4.0, 2)));
        when(placeRepository.findByAllTagsAdequate(tags, 2L, pagina)).thenReturn(filtrados);

        assertThat(placeService.search(tags, pagina)).isSameAs(filtrados);

        verify(placeRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void searchSemResultadoDeveDevolverPaginaVazia() {
        Pageable pagina = PageRequest.of(0, 20);
        Set<AccessibilityTag> tags = Set.of(AccessibilityTag.ELEVADORES);
        when(placeRepository.findByAllTagsAdequate(tags, 1L, pagina)).thenReturn(Page.empty(pagina));

        Page<Place> resultado = placeService.search(tags, pagina);

        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
    }
}
