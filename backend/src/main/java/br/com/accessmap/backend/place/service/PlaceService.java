package br.com.accessmap.backend.place.service;

import br.com.accessmap.backend.place.dto.PlaceSummaryDto;
import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.model.TagStats;
import br.com.accessmap.backend.place.repository.PlaceRepository;
import br.com.accessmap.backend.review.enums.AccessibilityTag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;

    public Place findByPlaceId(String placeId) {
        return placeRepository.findByPlaceId(placeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Local não encontrado"));
    }

    /** Sem tags, lista todos os locais; com tags, só os que têm todas elas (ver {@link PlaceRepository#findByAllTagsAdequate}). */
    public Page<Place> search(Set<AccessibilityTag> tags, Pageable pageable) {
        if (tags == null || tags.isEmpty()) {
            return placeRepository.findAll(pageable);
        }
        return placeRepository.findByAllTagsAdequate(tags, tags.size(), pageable);
    }

    /**
     * Agregados de vários locais numa chamada só, na mesma ordem de {@code placeIds} (o mobile manda ordenado
     * por distância). Sem tags, local sem avaliação volta zerado; com tags, só voltam os que passam no filtro.
     */
    public List<PlaceSummaryDto> findBatch(List<String> placeIds, Set<AccessibilityTag> tags) {
        List<String> ordem = placeIds.stream().distinct().toList();
        boolean filtrar = tags != null && !tags.isEmpty();

        Map<String, Place> conhecidos = (filtrar
                ? placeRepository.findByPlaceIdInAndAllTagsAdequate(ordem, tags, tags.size())
                : placeRepository.findByPlaceIdIn(ordem))
                .stream().collect(Collectors.toMap(Place::getPlaceId, Function.identity()));

        return ordem.stream()
                .map(id -> {
                    Place place = conhecidos.get(id);
                    if (place != null) {
                        return PlaceSummaryDto.from(place);
                    }
                    return filtrar ? null : PlaceSummaryDto.semAvaliacoes(id);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public Place findOrCreateByPlaceId(String placeId) {
        return placeRepository.findByPlaceId(placeId)
                .orElseGet(() -> placeRepository.save(
                        Place.builder()
                                .placeId(placeId)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                ));
    }

    /**
     * Sobrescreve os agregados do local com os totais recalculados a partir das avaliações publicadas.
     * A média é derivada de soma/contagem para não acumular erro de ponto flutuante.
     */
    public void applyAggregates(String placeId, long reviewCount, long ratingSum,
                                Map<AccessibilityTag, TagStats> tagStats) {
        Place place = findOrCreateByPlaceId(placeId);

        place.setReviewCount((int) reviewCount);
        place.setAverageScore(reviewCount > 0 ? (double) ratingSum / reviewCount : 0.0);

        if (place.getTagStats() == null) {
            place.setTagStats(tagStats);
        } else {
            place.getTagStats().clear();
            place.getTagStats().putAll(tagStats);
        }

        place.setUpdatedAt(LocalDateTime.now());

        placeRepository.save(place);
    }
}
