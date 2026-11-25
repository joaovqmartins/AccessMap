package br.com.accessmap.backend.auth.controller;

import br.com.accessmap.backend.auth.dto.PasswordResetConfirmDto;
import br.com.accessmap.backend.auth.dto.PasswordResetRequestDto;
import br.com.accessmap.backend.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    public static final String CODIGO_ENVIADO = "Se houver uma conta com esse e-mail, enviamos um código.";

    private final PasswordResetService passwordResetService;

    @Operation(summary = "Envia um código de 6 dígitos para o e-mail da conta",
            description = "Responde igual exista ou não a conta, para não revelar quem está cadastrado. "
                    + "Pedir de novo invalida o código anterior.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Pedido aceito"),
            @ApiResponse(responseCode = "400", description = "E-mail inválido"),
            @ApiResponse(responseCode = "429", description = "Muitos pedidos para o mesmo e-mail ou IP")
    })
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> request(@Valid @RequestBody PasswordResetRequestDto request,
                                                       HttpServletRequest httpRequest) {
        passwordResetService.request(request.getEmail(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("message", CODIGO_ENVIADO));
    }

    @Operation(summary = "Troca a senha com o código recebido por e-mail",
            description = "Encerra todas as sessões da conta. Depois de 5 códigos errados é preciso pedir outro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha alterada"),
            @ApiResponse(responseCode = "400", description = "Código inválido ou expirado, ou senha inválida")
    })
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirm(@Valid @RequestBody PasswordResetConfirmDto request) {
        passwordResetService.confirm(request.getEmail(), request.getCode(), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Senha alterada"));
    }
}
