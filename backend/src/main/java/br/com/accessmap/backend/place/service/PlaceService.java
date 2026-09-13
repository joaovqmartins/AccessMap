package br.com.accessmap.backend.place.service;

import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.model.TagStats;
import br.com.accessmap.backend.place.repository.PlaceRepository;
import br.com.accessmap.backend.review.enums.AccessibilityTag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;

    public Place findByPlaceId(String placeId) {
        return placeRepository.findByPlaceId(placeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Local não encontrado"));
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
