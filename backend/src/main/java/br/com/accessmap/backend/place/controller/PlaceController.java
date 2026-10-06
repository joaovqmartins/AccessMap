package br.com.accessmap.backend.place.controller;

import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.service.PlaceService;
import br.com.accessmap.backend.review.enums.AccessibilityTag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@Tag(name = "Locais", description = "Consulta de locais avaliados: cache de agregados sobre o Google Place ID")
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @Operation(summary = "Busca locais por características de acessibilidade",
            description = "Devolve os locais que o AccessMap conhece e que têm **todas** as características em `tags`. "
                    + "Um local \"tem\" a característica quando a maioria estrita dos votos nela é ADEQUADO. "
                    + "Sem `tags`, lista todos. Paginado; por padrão ordena pela nota média e depois pelo total de avaliações.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de locais"),
            @ApiResponse(responseCode = "400", description = "Tag inexistente em `tags`")
    })
    @GetMapping
    public ResponseEntity<Page<Place>> search(
            @Parameter(description = "Características separadas por vírgula, ex: RAMPAS_E_ENTRADAS,ELEVADORES")
            @RequestParam(required = false) Set<AccessibilityTag> tags,
            @PageableDefault(size = 20, sort = {"averageScore", "reviewCount"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(placeService.search(tags, pageable));
    }

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
