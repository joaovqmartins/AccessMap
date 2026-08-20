package br.com.accessmap.backend.place.controller;

import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping("/{placeId}")
    public ResponseEntity<Place> getByPlaceId(@PathVariable String placeId) {
        return ResponseEntity.ok(placeService.findByPlaceId(placeId));
    }
}
