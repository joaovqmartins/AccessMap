package br.com.accessmap.backend.identity.controller;

import br.com.accessmap.backend.identity.dto.UserRequestDto;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Usuários", description = "Conta do usuário autenticado e administração de contas")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ---------- usuário autenticado ----------

    @Operation(summary = "Dados do usuário autenticado")
    @GetMapping("/me")
    public ResponseEntity<User> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.findById(jwt.getSubject()));
    }

    @Operation(summary = "Atualiza parcialmente os dados do usuário autenticado",
            description = "Para trocar a senha, envie password e currentPassword. "
                    + "A troca de senha encerra todas as sessões (refresh tokens) do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado"),
            @ApiResponse(responseCode = "400", description = "E-mail já em uso ou senha atual incorreta")
    })
    @PatchMapping("/me")
    public ResponseEntity<User> updateMe(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody UserRequestDto request) {
        return ResponseEntity.ok(userService.update(jwt.getSubject(), request));
    }

    @Operation(summary = "Remove a conta do usuário autenticado")
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteMe(@AuthenticationPrincipal Jwt jwt) {
        userService.delete(jwt.getSubject());
        return ResponseEntity.ok(Map.of("message", "Conta removida com sucesso"));
    }

    // ---------- administração ----------

    @Operation(summary = "Lista todos os usuários (ADMIN)")
    @ApiResponse(responseCode = "403", description = "Requer perfil ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<User>> listAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(summary = "Busca um usuário pelo ID (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "403", description = "Requer perfil ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@Parameter(description = "ID do usuário") @PathVariable String id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Operation(summary = "Remove um usuário (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário removido"),
            @ApiResponse(responseCode = "403", description = "Requer perfil ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@Parameter(description = "ID do usuário") @PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Usuário removido com sucesso"));
    }
}
