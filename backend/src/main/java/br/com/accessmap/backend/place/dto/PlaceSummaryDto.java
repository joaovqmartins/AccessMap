package br.com.accessmap.backend.place.dto;

import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.model.TagStats;
import br.com.accessmap.backend.review.enums.AccessibilityTag;

import java.util.EnumMap;
import java.util.Map;

public record PlaceSummaryDto(String placeId, double averageScore, int reviewCount,
                              Map<AccessibilityTag, TagStats> tagStats) {

    public static PlaceSummaryDto from(Place place) {
        return new PlaceSummaryDto(place.getPlaceId(), place.getAverageScore(), place.getReviewCount(),
                place.getTagStats());
    }

    /** Local que o Google conhece mas ninguém avaliou no AccessMap ainda. */
    public static PlaceSummaryDto semAvaliacoes(String placeId) {
        return new PlaceSummaryDto(placeId, 0.0, 0, new EnumMap<>(AccessibilityTag.class));
    }
}
