package br.com.accessmap.backend.exception;

import br.com.accessmap.backend.identity.service.UserService;
import br.com.accessmap.backend.review.service.ReviewService;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    /** Mesma cadeia que o Spring monta quando o Postgres barra um INSERT pela constraint. */
    private static DataIntegrityViolationException violacao(String constraint) {
        SQLException sql = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"" + constraint + "\"", "23505");
        ConstraintViolationException hibernate = new ConstraintViolationException(
                "could not execute statement", sql, constraint);
        return new DataIntegrityViolationException("could not execute statement", hibernate);
    }

    static Stream<Arguments> unicidadesConhecidas() {
        return Stream.of(
                Arguments.of("uk_users_phone", UserService.TELEFONE_EM_USO),
                Arguments.of("uk_users_email", UserService.EMAIL_EM_USO),
                Arguments.of("uk_reviews_user_place_active", ReviewService.LOCAL_JA_AVALIADO)
        );
    }

    @ParameterizedTest
    @MethodSource("unicidadesConhecidas")
    void unicidadeConhecidaRespondeConflito(String constraint, String mensagem) {
        ResponseEntity<Map<String, Object>> resposta = handler.handleDataIntegrity(violacao(constraint));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "status", 409,
                "mensagem", mensagem
        ));
    }

    @Test
    void constraintDesconhecidaRespondeErroInterno() {
        ResponseEntity<Map<String, Object>> resposta = handler.handleDataIntegrity(violacao("uk_places_place_id"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "status", 500,
                "mensagem", "Erro interno no servidor"
        ));
    }

    @Test
    void violacaoSemConstraintIdentificadaRespondeErroInterno() {
        ResponseEntity<Map<String, Object>> resposta = handler.handleDataIntegrity(
                new DataIntegrityViolationException("not-null violado"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
