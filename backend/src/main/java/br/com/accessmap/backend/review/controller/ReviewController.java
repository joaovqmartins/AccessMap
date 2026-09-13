package br.com.accessmap.backend.review.controller;

import br.com.accessmap.backend.review.dto.ReviewRequestDto;
import br.com.accessmap.backend.review.dto.ReviewUpdateRequestDto;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Avaliações", description = "Criação e consulta de avaliações de acessibilidade")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Lista as avaliações de um local")
    @GetMapping
    public ResponseEntity<List<Review>> list(@RequestParam(required = false) String placeId,
                                             @RequestParam(required = false) String userId) {
        boolean hasPlace = placeId != null && !placeId.isBlank();
        boolean hasUser = userId != null && !userId.isBlank();

        if (hasPlace && hasUser) {
            return ResponseEntity.ok(reviewService.findByUserIdAndPlaceId(userId, placeId));
        }
        if (hasPlace) {
            return ResponseEntity.ok(reviewService.findByPlaceId(placeId));
        }
        if (hasUser) {
            return ResponseEntity.ok(reviewService.findByUserId(userId));
        }
        return ResponseEntity.ok(reviewService.findAll());
    }

    @Operation(summary = "Busca uma avaliação pelo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avaliação encontrada"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Review> getById(@Parameter(description = "ID da avaliação") @PathVariable String id) {
        return ResponseEntity.ok(reviewService.findById(id));
    }

    @Operation(summary = "Cria uma avaliação de acessibilidade para um local",
            description = "O autor é o usuário autenticado. Um usuário só pode ter uma avaliação publicada por local.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Avaliação criada"),
            @ApiResponse(responseCode = "400", description = "Campo obrigatório ausente, nota fora de 1-5 ou nenhuma tag"),
            @ApiResponse(responseCode = "401", description = "Requer autenticação"),
            @ApiResponse(responseCode = "409", description = "Usuário já avaliou este local")
    })
    @PostMapping
    public ResponseEntity<Review> create(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody ReviewRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(jwt.getSubject(), request));
    }

    @Operation(summary = "Atualiza parcialmente uma avaliação",
            description = "Somente o autor (ou um ADMIN) pode alterar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avaliação atualizada"),
            @ApiResponse(responseCode = "403", description = "A avaliação pertence a outro usuário"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<Review> update(@AuthenticationPrincipal Jwt jwt,
                                         @PathVariable String id,
                                         @Valid @RequestBody ReviewUpdateRequestDto request) {
        return ResponseEntity.ok(reviewService.update(id, jwt.getSubject(), isAdmin(jwt), request));
    }

    @Operation(summary = "Remove uma avaliação",
            description = "Somente o autor (ou um ADMIN) pode remover. A avaliação deixa de ser listada, mas é preservada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avaliação removida"),
            @ApiResponse(responseCode = "403", description = "A avaliação pertence a outro usuário"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        reviewService.delete(id, jwt.getSubject(), isAdmin(jwt));
        return ResponseEntity.ok(Map.of("message", "Avaliação removida com sucesso"));
    }

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
