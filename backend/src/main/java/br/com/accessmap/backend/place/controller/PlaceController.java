package br.com.accessmap.backend.place.controller;

import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Locais", description = "Consulta de locais avaliados: cache de agregados sobre o Google Place ID")
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @Operation(summary = "Busca um local pelo Google Place ID, com a nota média e o total de avaliações")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Local encontrado"),
            @ApiResponse(responseCode = "404", description = "Nenhuma avaliação registrada para esse Place ID ainda")
    })
    @GetMapping("/{placeId}")
    public ResponseEntity<Place> getByPlaceId(
            @Parameter(description = "Place ID do Google Maps Platform") @PathVariable String placeId) {
        return ResponseEntity.ok(placeService.findByPlaceId(placeId));
    }
}
