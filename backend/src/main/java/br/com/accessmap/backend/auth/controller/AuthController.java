package br.com.accessmap.backend.auth.controller;

import br.com.accessmap.backend.auth.dto.LoginRequestDto;
import br.com.accessmap.backend.auth.dto.RefreshRequestDto;
import br.com.accessmap.backend.auth.dto.RegisterRequestDto;
import br.com.accessmap.backend.auth.dto.TokenResponseDto;
import br.com.accessmap.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Autenticação", description = "Cadastro, login e ciclo de vida dos tokens")
@SecurityRequirements // endpoints públicos: sobrescreve o requisito global de bearer no Swagger
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Cadastra um usuário e já devolve os tokens de acesso")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado e autenticado"),
            @ApiResponse(responseCode = "400", description = "Campo inválido ou e-mail já em uso")
    })
    @PostMapping("/register")
    public ResponseEntity<TokenResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Autentica com e-mail e senha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens emitidos"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Troca um refresh token válido por um novo par de tokens",
            description = "O refresh token apresentado é invalidado (rotação). "
                    + "Reapresentar um token já usado derruba todas as sessões do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Novo par de tokens emitido"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado ou já utilizado")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(@Valid @RequestBody RefreshRequestDto request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @Operation(summary = "Encerra a sessão revogando o refresh token",
            description = "O access token continua válido até expirar (15 min). Idempotente.")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshRequestDto request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Sessão encerrada"));
    }
}
