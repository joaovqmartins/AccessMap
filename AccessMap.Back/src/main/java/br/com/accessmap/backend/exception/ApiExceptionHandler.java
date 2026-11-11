package br.com.accessmap.backend.exception;

import br.com.accessmap.backend.identity.service.UserService;
import br.com.accessmap.backend.review.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    // Erros de validação (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                campos.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "mensagem", "Erro de validação",
                "campos", campos
        ));
    }

    // Parâmetro de URL com valor inválido (ex: ?tags=BANANA) é erro do cliente, não 500
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> tipo = ex.getRequiredType();
        if (tipo != null && Collection.class.isAssignableFrom(tipo)) {
            // parâmetro como Set<Enum>: o tipo que importa é o do elemento, não o da coleção
            tipo = ex.getParameter().nested().getNestedParameterType();
        }
        String mensagem = "Valor inválido para o parâmetro '" + ex.getName() + "': " + ex.getValue();
        if (tipo != null && tipo.isEnum()) {
            mensagem += ". Valores aceitos: " + Arrays.toString(tipo.getEnumConstants());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "mensagem", mensagem
        ));
    }

    // Corpo JSON ilegível (sintaxe quebrada ou valor de enum inexistente) é erro do cliente, não 500
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        String mensagem = "Corpo da requisição inválido";
        if (ex.getCause() instanceof InvalidFormatException formato) {
            Class<?> tipo = formato.getTargetType();
            mensagem += ": valor '" + formato.getValue() + "' não é válido";
            if (tipo != null && tipo.isEnum()) {
                mensagem += ". Valores aceitos: " + Arrays.toString(tipo.getEnumConstants());
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "mensagem", mensagem
        ));
    }

    // Erros de negócio (ResponseStatusException)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                "status", ex.getStatusCode().value(),
                "mensagem", message
        ));
    }

    // Acesso negado dentro do controller (@PreAuthorize) — o handler do filtro não alcança aqui
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", 403,
                "mensagem", "Acesso negado"
        ));
    }

    /**
     * Constraints de unicidade que o serviço já checa antes de salvar. Entre a checagem e o INSERT
     * há uma janela: duas requisições simultâneas (toque duplo, reenvio do app) passam pela checagem
     * e o banco barra a segunda. A resposta é a mesma da checagem do serviço.
     */
    private static final Map<String, String> CONFLITOS_DE_UNICIDADE = Map.of(
            "uk_users_phone", UserService.TELEFONE_EM_USO,
            "uk_users_email", UserService.EMAIL_EM_USO,
            "uk_reviews_user_place_active", ReviewService.LOCAL_JA_AVALIADO
    );

    // Violação de constraint no banco: as de unicidade conhecidas são 409, o resto é erro interno
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String constraint = nomeDaConstraint(ex);
        String mensagem = constraint == null ? null : CONFLITOS_DE_UNICIDADE.get(constraint);
        if (mensagem == null) {
            return handleGeneric(ex);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", 409,
                "mensagem", mensagem
        ));
    }

    private static String nomeDaConstraint(Throwable ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacao && violacao.getConstraintName() != null) {
                return violacao.getConstraintName().toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    // Erros inesperados (segurança — não expõe detalhes internos)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Erro inesperado ao processar requisição", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", 500,
                "mensagem", "Erro interno no servidor"
        ));
    }
}
