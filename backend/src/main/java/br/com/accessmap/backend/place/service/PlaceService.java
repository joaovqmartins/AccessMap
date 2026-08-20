package br.com.accessmap.backend.place.service;

import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

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

    public void registerNewReview(String placeId, int rating) {
        Place place = findOrCreateByPlaceId(placeId);

        double totalScore = place.getAverageScore() * place.getReviewCount() + rating;
        int newCount = place.getReviewCount() + 1;

        place.setReviewCount(newCount);
        place.setAverageScore(totalScore / newCount);
        place.setUpdatedAt(LocalDateTime.now());

        placeRepository.save(place);
    }
}
