package br.com.accessmap.backend.auth.service;

import br.com.accessmap.backend.auth.config.AuthRateLimitProperties;
import br.com.accessmap.backend.auth.enums.AuthAttemptKind;
import br.com.accessmap.backend.auth.model.AuthAttempt;
import br.com.accessmap.backend.auth.repository.AuthAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthAttemptServiceTest {

    private static final String TELEFONE = "11999990000";
    private static final String IP = "10.0.0.1";

    @Mock
    private AuthAttemptRepository repository;

    private AuthAttemptService service;

    @BeforeEach
    void montar() {
        service = new AuthAttemptService(repository, new AuthRateLimitProperties(Duration.ofMinutes(15), 5, 20, 10));
    }

    private void tentativas(AuthAttemptKind kind, long total) {
        lenient().when(repository.countByKindAndKeyHashAndCreatedAtAfter(eq(kind), anyString(), any())).thenReturn(total);
    }

    private void maisAntigaHa(AuthAttemptKind kind, Duration tempo) {
        when(repository.findFirstByKindAndKeyHashAndCreatedAtAfterOrderByCreatedAtAsc(eq(kind), anyString(), any()))
                .thenReturn(Optional.of(AuthAttempt.builder().createdAt(LocalDateTime.now().minus(tempo)).build()));
    }

    private ResponseStatusException assertBloqueiaLogin() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.assertLoginAllowed(TELEFONE, IP));
        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        return erro;
    }

    @Test
    void abaixoDosLimitesOLoginPassa() {
        tentativas(AuthAttemptKind.LOGIN_PHONE, 4);
        tentativas(AuthAttemptKind.LOGIN_IP, 19);

        assertThatCode(() -> service.assertLoginAllowed(TELEFONE, IP)).doesNotThrowAnyException();
    }

    @Test
    void noLimitePorTelefoneBloqueiaEDizQuantoFalta() {
        tentativas(AuthAttemptKind.LOGIN_PHONE, 5);
        maisAntigaHa(AuthAttemptKind.LOGIN_PHONE, Duration.ofMinutes(3));

        assertThat(assertBloqueiaLogin().getReason()).isEqualTo("Muitas tentativas. Tente novamente em 12 minutos.");
    }

    @Test
    void noLimitePorIpBloqueia() {
        tentativas(AuthAttemptKind.LOGIN_PHONE, 0);
        tentativas(AuthAttemptKind.LOGIN_IP, 20);
        maisAntigaHa(AuthAttemptKind.LOGIN_IP, Duration.ofMinutes(1));

        assertBloqueiaLogin();
    }

    @Test
    void faltandoMenosDeUmMinutoUsaOSingular() {
        tentativas(AuthAttemptKind.LOGIN_PHONE, 5);
        maisAntigaHa(AuthAttemptKind.LOGIN_PHONE, Duration.ofMinutes(14).plusSeconds(30));

        assertThat(assertBloqueiaLogin().getReason()).isEqualTo("Muitas tentativas. Tente novamente em 1 minuto.");
    }

    @Test
    void noLimiteDeCadastroBloqueia() {
        tentativas(AuthAttemptKind.REGISTER_IP, 10);
        maisAntigaHa(AuthAttemptKind.REGISTER_IP, Duration.ofMinutes(1));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.assertRegisterAllowed(IP));

        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void falhaDeLoginRegistraTelefoneEIpSoComHash() {
        service.recordLoginFailure(TELEFONE, IP);

        ArgumentCaptor<AuthAttempt> salvas = ArgumentCaptor.forClass(AuthAttempt.class);
        verify(repository, times(2)).save(salvas.capture());
        assertThat(salvas.getAllValues()).extracting(AuthAttempt::getKind)
                .containsExactly(AuthAttemptKind.LOGIN_PHONE, AuthAttemptKind.LOGIN_IP);
        assertThat(salvas.getAllValues()).extracting(AuthAttempt::getKeyHash)
                .doesNotContain(TELEFONE, IP)
                .allSatisfy(hash -> assertThat(hash).hasSize(64));
    }

    @Test
    void falhaApagaOsRegistrosForaDaJanela() {
        service.recordRegisterFailure(IP);

        verify(repository).deleteByCreatedAtBefore(any());
    }

    @Test
    void loginCorretoZeraSoOTelefone() {
        service.clearLoginFailures(TELEFONE);

        verify(repository).deleteByKindAndKeyHash(eq(AuthAttemptKind.LOGIN_PHONE), anyString());
        verify(repository, never()).deleteByKindAndKeyHash(eq(AuthAttemptKind.LOGIN_IP), anyString());
    }
}
