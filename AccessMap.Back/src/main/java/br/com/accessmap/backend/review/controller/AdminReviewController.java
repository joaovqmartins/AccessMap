package br.com.accessmap.backend.review.controller;

import br.com.accessmap.backend.review.dto.AdminReviewDto;
import br.com.accessmap.backend.review.service.ReviewModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Moderação", description = "Fila de avaliações suspeitas, só para ADMIN")
@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewModerationService moderationService;

    @Operation(summary = "Lista a fila de moderação (ADMIN)",
            description = "Avaliações ocultas por denúncias e publicadas com nota destoante, das mais antigas para as mais novas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página da fila"),
            @ApiResponse(responseCode = "401", description = "Requer autenticação"),
            @ApiResponse(responseCode = "403", description = "Requer perfil ADMIN")
    })
    @GetMapping
    public ResponseEntity<Page<AdminReviewDto>> queue(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(moderationService.queue(pageable));
    }
}
