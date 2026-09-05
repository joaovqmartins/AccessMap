package br.com.accessmap.backend.review.controller;

import br.com.accessmap.backend.review.dto.ReviewRequestDto;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Avaliações", description = "Criação e consulta de avaliações de acessibilidade")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Lista as avaliações de um local")
    @GetMapping
    public ResponseEntity<List<Review>> listByPlace(
            @Parameter(description = "Place ID do Google Maps Platform") @RequestParam String placeId) {
        return ResponseEntity.ok(reviewService.findByPlaceId(placeId));
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

    @Operation(summary = "Cria uma nova avaliação de acessibilidade para um local")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Avaliação criada"),
            @ApiResponse(responseCode = "400", description = "Campo obrigatório ausente ou nota fora do intervalo 1-5"),
            @ApiResponse(responseCode = "404", description = "Usuário informado não existe")
    })
    @PostMapping
    public ResponseEntity<Review> create(@Valid @RequestBody ReviewRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request));
    }
}
