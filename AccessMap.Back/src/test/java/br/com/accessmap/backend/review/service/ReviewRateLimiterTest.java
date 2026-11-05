package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.review.config.ReviewRateLimitProperties;
import br.com.accessmap.backend.review.repository.ReviewRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ReviewRateLimiterTest {

    private static final int POR_HORA = 5;
    private static final int POR_DIA = 20;
    private static final int NOVA_POR_HORA = 3;
    private static final int NOVA_POR_DIA = 10;

    @Mock
    private ReviewRepository reviewRepository;

    private ReviewRateLimiter limiter;

    @BeforeEach
    void montar() {
        limiter = new ReviewRateLimiter(reviewRepository, new ReviewRateLimitProperties(
                POR_HORA, POR_DIA, NOVA_POR_HORA, NOVA_POR_DIA, Duration.ofDays(7)));
    }

    private User usuario(Role role, LocalDateTime criadoEm) {
        return User.builder().id("u1").role(role).createdAt(criadoEm).build();
    }

    private User contaAntiga() {
        return usuario(Role.USER, LocalDateTime.now().minusDays(30));
    }

    private User contaNova() {
        return usuario(Role.USER, LocalDateTime.now().minusDays(1));
    }

    /** A primeira contagem é a da última hora; a segunda, a das últimas 24 horas. */
    private void criadas(long naUltimaHora, long noUltimoDia) {
        lenient().when(reviewRepository.countByUserIdAndCreatedAtAfter(eq("u1"), any()))
                .thenReturn(naUltimaHora, noUltimoDia);
    }

    private void assertBloqueia(User user) {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> limiter.check(user));
        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void contaAntigaAbaixoDosLimitesPassa() {
        criadas(POR_HORA - 1, POR_DIA - 1);

        assertThatCode(() -> limiter.check(contaAntiga())).doesNotThrowAnyException();
    }

    @Test
    void contaAntigaNoLimitePorHoraEBloqueada() {
        criadas(POR_HORA, POR_HORA);

        assertBloqueia(contaAntiga());
    }

    @Test
    void contaAntigaNoLimitePorDiaEBloqueada() {
        criadas(0, POR_DIA);

        assertBloqueia(contaAntiga());
    }

    @Test
    void contaNovaTemLimitePorHoraMenor() {
        criadas(NOVA_POR_HORA, NOVA_POR_HORA);

        assertBloqueia(contaNova());
    }

    @Test
    void contaNovaTemLimitePorDiaMenor() {
        criadas(0, NOVA_POR_DIA);

        assertBloqueia(contaNova());
    }

    @Test
    void contaQueJaPassouDoPrazoDeNovaUsaOLimiteNormal() {
        criadas(NOVA_POR_HORA, NOVA_POR_DIA);

        assertThatCode(() -> limiter.check(usuario(Role.USER, LocalDateTime.now().minusDays(8))))
                .doesNotThrowAnyException();
    }

    @Test
    void contaSemDataDeCriacaoUsaOLimiteNormal() {
        criadas(NOVA_POR_HORA, NOVA_POR_DIA);

        assertThatCode(() -> limiter.check(usuario(Role.USER, null))).doesNotThrowAnyException();
    }

    @Test
    void adminNaoTemLimiteNemConsultaAContagem() {
        limiter.check(usuario(Role.ADMIN, LocalDateTime.now()));

        verifyNoInteractions(reviewRepository);
    }

    @Test
    void contaAsJanelasDeUmaHoraEDeUmDia() {
        criadas(0, 0);

        limiter.check(contaAntiga());

        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(reviewRepository, atLeastOnce()).countByUserIdAndCreatedAtAfter(eq("u1"), desde.capture());
        List<LocalDateTime> janelas = desde.getAllValues();
        assertThat(janelas).hasSize(2);
        assertThat(janelas.get(0)).isCloseTo(LocalDateTime.now().minusHours(1), within(5, java.time.temporal.ChronoUnit.SECONDS));
        assertThat(janelas.get(1)).isCloseTo(LocalDateTime.now().minusDays(1), within(5, java.time.temporal.ChronoUnit.SECONDS));
    }
}
