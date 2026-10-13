package br.com.accessmap.backend.place.controller;

import br.com.accessmap.backend.place.dto.PlaceBatchRequestDto;
import br.com.accessmap.backend.place.dto.PlaceSummaryDto;
import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.service.PlaceService;
import br.com.accessmap.backend.review.enums.AccessibilityTag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
                    + "Com `useMyNeeds=true` (exige login), soma às `tags` as essenciais para as necessidades de "
                    + "acessibilidade do perfil. Sem nenhuma tag, lista todos. "
                    + "Paginado; por padrão ordena pela nota média e depois pelo total de avaliações.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de locais"),
            @ApiResponse(responseCode = "400", description = "Tag inexistente em `tags`"),
            @ApiResponse(responseCode = "401", description = "`useMyNeeds=true` sem estar logado")
    })
    @GetMapping
    public ResponseEntity<Page<Place>> search(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Características separadas por vírgula, ex: RAMPAS_E_ENTRADAS,ELEVADORES")
            @RequestParam(required = false) Set<AccessibilityTag> tags,
            @Parameter(description = "Somar as tags essenciais para as necessidades do usuário logado")
            @RequestParam(defaultValue = "false") boolean useMyNeeds,
            @PageableDefault(size = 20, sort = {"averageScore", "reviewCount"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        Set<AccessibilityTag> efetivas = placeService.effectiveTags(tags, useMyNeeds, userId(jwt));
        return ResponseEntity.ok(placeService.search(efetivas, pageable));
    }

    @Operation(summary = "Agregados de vários locais numa chamada só",
            description = "Recebe os `placeId`s que o app já obteve do Google (ordenados por distância) e devolve "
                    + "nota média, total de avaliações e estatísticas por característica de cada um, **na mesma ordem**. "
                    + "Local que ninguém avaliou ainda volta zerado, sem 404. Com `tags`, só voltam os locais que têm "
                    + "todas elas (mesma regra de `GET /api/places`); `useMyNeeds` funciona como na busca. "
                    + "Máximo de 60 `placeId`s.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo de cada local, na ordem pedida"),
            @ApiResponse(responseCode = "400", description = "Lista vazia, com mais de 60 itens, ou tag inexistente"),
            @ApiResponse(responseCode = "401", description = "`useMyNeeds: true` sem estar logado")
    })
    @PostMapping("/batch")
    public ResponseEntity<List<PlaceSummaryDto>> batch(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody PlaceBatchRequestDto request) {
        Set<AccessibilityTag> efetivas = placeService.effectiveTags(request.getTags(), request.isUseMyNeeds(), userId(jwt));
        return ResponseEntity.ok(placeService.findBatch(request.getPlaceIds(), efetivas));
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

    /** As rotas de busca são públicas: sem token, não há usuário. */
    private static String userId(Jwt jwt) {
        return jwt != null ? jwt.getSubject() : null;
    }
}
