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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @Operation(summary = "Restaura uma avaliação oculta (ADMIN)",
            description = "Volta para PUBLICADA, recalcula a nota do local e não pode mais ser ocultada por denúncias.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Avaliação restaurada"),
            @ApiResponse(responseCode = "403", description = "Requer perfil ADMIN"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada"),
            @ApiResponse(responseCode = "409", description = "A avaliação não está oculta")
    })
    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable String id) {
        moderationService.restore(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mantém no ar uma avaliação sinalizada (ADMIN)",
            description = "Tira da fila uma avaliação publicada. Denúncias posteriores ainda podem ocultá-la.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Avaliação mantida"),
            @ApiResponse(responseCode = "403", description = "Requer perfil ADMIN"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada"),
            @ApiResponse(responseCode = "409", description = "A avaliação não está aguardando moderação")
    })
    @PostMapping("/{id}/keep")
    public ResponseEntity<Void> keep(@PathVariable String id) {
        moderationService.keep(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove uma avaliação pela moderação (ADMIN)",
            description = "Soft delete que conta para o nível Restrita do autor.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Avaliação removida"),
            @ApiResponse(responseCode = "403", description = "Requer perfil ADMIN"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada ou já removida")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable String id) {
        moderationService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
