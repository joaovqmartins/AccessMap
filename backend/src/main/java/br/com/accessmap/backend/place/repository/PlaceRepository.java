package br.com.accessmap.backend.place.repository;

import br.com.accessmap.backend.place.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, String> {
    Optional<Place> findByPlaceId(String placeId);
}
