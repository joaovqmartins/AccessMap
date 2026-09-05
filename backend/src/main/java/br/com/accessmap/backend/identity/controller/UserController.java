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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Usuários", description = "Cadastro e gerenciamento de contas de usuário")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Lista todos os usuários cadastrados")
    @GetMapping
    public ResponseEntity<List<User>> listAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(summary = "Busca um usuário pelo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@Parameter(description = "ID do usuário") @PathVariable String id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Operation(summary = "Cadastra um novo usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado"),
            @ApiResponse(responseCode = "400", description = "Campo obrigatório ausente, inválido ou e-mail já em uso")
    })
    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody UserRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @Operation(summary = "Atualiza parcialmente os dados de um usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado"),
            @ApiResponse(responseCode = "400", description = "E-mail já em uso por outro usuário"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<User> update(@Valid @PathVariable String id, @RequestBody UserRequestDto request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @Operation(summary = "Remove um usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário removido"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@Parameter(description = "ID do usuário") @PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Usuário removido com sucesso"));
    }
}
