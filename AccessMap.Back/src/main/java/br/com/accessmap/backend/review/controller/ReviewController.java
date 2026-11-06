package br.com.accessmap.backend.review.controller;

import br.com.accessmap.backend.review.dto.ReviewReportRequestDto;
import br.com.accessmap.backend.review.dto.ReviewRequestDto;
import br.com.accessmap.backend.review.dto.ReviewResponseDto;
import br.com.accessmap.backend.review.dto.ReviewUpdateRequestDto;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.service.ReviewReportService;
import br.com.accessmap.backend.review.service.ReviewService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Tag(name = "Avaliações", description = "Criação e consulta de avaliações de acessibilidade")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewReportService reviewReportService;

    @Operation(summary = "Lista as avaliações de um local",
            description = "Paginado; por padrão ordenado das mais recentes para as mais antigas. "
                    + "O filtro `userId` exige login e só vale para o próprio usuário ou para um ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de avaliações"),
            @ApiResponse(responseCode = "401", description = "Filtro `userId` sem estar logado"),
            @ApiResponse(responseCode = "403", description = "Filtro `userId` de outro usuário")
    })
    @GetMapping
    public ResponseEntity<Page<ReviewResponseDto>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String placeId,
            @RequestParam(required = false) String userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        boolean hasPlace = placeId != null && !placeId.isBlank();
        boolean hasUser = userId != null && !userId.isBlank();
        if (hasUser) {
            assertCanFilterByUser(jwt, userId);
        }

        Page<Review> page;
        if (hasPlace && hasUser) {
            page = reviewService.findByUserIdAndPlaceId(userId, placeId, pageable);
        } else if (hasPlace) {
            page = reviewService.findByPlaceId(placeId, pageable);
        } else if (hasUser) {
            page = reviewService.findByUserId(userId, pageable);
        } else {
            page = reviewService.findAll(pageable);
        }
        return ResponseEntity.ok(page.map(review -> ReviewResponseDto.from(review, viewerId(jwt))));
    }

    @Operation(summary = "Busca uma avaliação pelo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avaliação encontrada"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponseDto> getById(@AuthenticationPrincipal Jwt jwt,
                                                     @Parameter(description = "ID da avaliação") @PathVariable String id) {
        return ResponseEntity.ok(ReviewResponseDto.from(reviewService.findById(id), viewerId(jwt)));
    }

    @Operation(summary = "Cria uma avaliação de acessibilidade para um local",
            description = "O autor é o usuário autenticado. Um usuário só pode ter uma avaliação publicada por local.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Avaliação criada"),
            @ApiResponse(responseCode = "400", description = "Campo obrigatório ausente, nota fora de 1-5 ou nenhuma tag"),
            @ApiResponse(responseCode = "401", description = "Requer autenticação"),
            @ApiResponse(responseCode = "409", description = "Usuário já avaliou este local"),
            @ApiResponse(responseCode = "429", description = "Limite de avaliações por hora ou por dia atingido")
    })
    @PostMapping
    public ResponseEntity<ReviewResponseDto> create(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody ReviewRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReviewResponseDto.from(reviewService.create(jwt.getSubject(), request), jwt.getSubject()));
    }

    @Operation(summary = "Atualiza parcialmente uma avaliação",
            description = "Somente o autor (ou um ADMIN) pode alterar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avaliação atualizada"),
            @ApiResponse(responseCode = "403", description = "A avaliação pertence a outro usuário"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ReviewResponseDto> update(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable String id,
                                                    @Valid @RequestBody ReviewUpdateRequestDto request) {
        Review updated = reviewService.update(id, jwt.getSubject(), isAdmin(jwt), request);
        return ResponseEntity.ok(ReviewResponseDto.from(updated, jwt.getSubject()));
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

    @Operation(summary = "Denuncia uma avaliação",
            description = "Quem denuncia é o usuário autenticado e não aparece em nenhuma resposta da API. "
                    + "A denúncia só fica registrada: não oculta a avaliação nem muda a nota do local.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Denúncia registrada"),
            @ApiResponse(responseCode = "400", description = "Motivo ausente ou inexistente"),
            @ApiResponse(responseCode = "401", description = "Requer autenticação"),
            @ApiResponse(responseCode = "403", description = "A avaliação é do próprio usuário"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada ou não publicada"),
            @ApiResponse(responseCode = "409", description = "Usuário já denunciou esta avaliação")
    })
    @PostMapping("/{id}/report")
    public ResponseEntity<Map<String, String>> report(@AuthenticationPrincipal Jwt jwt,
                                                      @PathVariable String id,
                                                      @Valid @RequestBody ReviewReportRequestDto request) {
        reviewReportService.report(id, jwt.getSubject(), request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Denúncia registrada"));
    }

    /** Listar as avaliações de alguém revela o histórico da pessoa: só ela mesma ou um ADMIN podem ver. */
    private void assertCanFilterByUser(Jwt jwt, String userId) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login para filtrar por usuário");
        }
        if (!userId.equals(jwt.getSubject()) && !isAdmin(jwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você só pode listar as suas próprias avaliações");
        }
    }

    /** As rotas de leitura são públicas: sem token, não há quem esteja vendo. */
    private static String viewerId(Jwt jwt) {
        return jwt != null ? jwt.getSubject() : null;
    }

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
