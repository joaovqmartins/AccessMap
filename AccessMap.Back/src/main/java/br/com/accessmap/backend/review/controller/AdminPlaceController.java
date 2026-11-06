package br.com.accessmap.backend.review.controller;

import br.com.accessmap.backend.review.service.ReviewModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Moderação", description = "Fila de avaliações suspeitas, só para ADMIN")
@RestController
@RequestMapping("/api/admin/places")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPlaceController {

    private final ReviewModerationService moderationService;

    @Operation(summary = "Recalcula a nota de todos os locais (ADMIN)",
            description = "Necessário depois de trocar um peso na configuração. Rodar de novo não muda nada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantidade de locais recalculados"),
            @ApiResponse(responseCode = "401", description = "Requer autenticação"),
            @ApiResponse(responseCode = "403", description = "Requer perfil ADMIN")
    })
    @PostMapping("/recalculate")
    public ResponseEntity<Map<String, Integer>> recalculate() {
        return ResponseEntity.ok(Map.of("recalculated", moderationService.recalculateAllPlaces()));
    }
}
