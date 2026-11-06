package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.review.config.ReviewRateLimitProperties;
import br.com.accessmap.backend.review.config.ReviewTrustProperties;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.enums.TrustLevel;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewTrustCalculatorTest {

    @Mock
    private ReviewRepository reviewRepository;

    private ReviewTrustCalculator calculator;

    @BeforeEach
    void montar() {
        calculator = new ReviewTrustCalculator(reviewRepository,
                new ReviewTrustProperties(Duration.ofDays(30), 5, Duration.ofDays(90), 2, 0.25, 0.5, 1.0, 1.5),
                new ReviewRateLimitProperties(5, 20, 3, 10, Duration.ofDays(7)));
    }

    private User contaCom(Duration idade) {
        return User.builder().id("u1").createdAt(LocalDateTime.now().minus(idade)).build();
    }

    private void historico(long publicadas, long removidasPelaModeracao) {
        lenient().when(reviewRepository.countByUserIdAndStatus("u1", ReviewStatus.PUBLICADA)).thenReturn(publicadas);
        lenient().when(reviewRepository.countByUserIdAndModerationRemovedAtAfter(eq("u1"), any()))
                .thenReturn(removidasPelaModeracao);
    }

    // ---------- cada nível ----------

    @Test
    void contaComHistoricoBomEConfiavel() {
        historico(5, 0);

        assertThat(calculator.levelOf(contaCom(Duration.ofDays(40)))).isEqualTo(TrustLevel.CONFIAVEL);
    }

    @Test
    void contaSemHistoricoEAntigaERegular() {
        historico(0, 0);

        assertThat(calculator.levelOf(contaCom(Duration.ofDays(40)))).isEqualTo(TrustLevel.REGULAR);
    }

    @Test
    void contaRecenteENova() {
        historico(0, 0);

        assertThat(calculator.levelOf(contaCom(Duration.ofDays(1)))).isEqualTo(TrustLevel.NOVA);
    }

    @Test
    void duasRemocoesDaModeracaoDeixamRestrita() {
        historico(10, 2);

        assertThat(calculator.levelOf(contaCom(Duration.ofDays(40)))).isEqualTo(TrustLevel.RESTRITA);
    }

    // ---------- limites exatos ----------

    @Test
    void depoisDeSeteDiasDeixaDeSerNova() {
        historico(0, 0);

        assertThat(calculator.levelOf(contaCom(Duration.ofDays(7).plusMinutes(1)))).isEqualTo(TrustLevel.REGULAR);
    }

    @Test
    void poucoAntesDeSeteDiasAindaENova() {
        historico(0, 0);

        assertThat(calculator.levelOf(contaCom(Duration.ofDays(7).minusMinutes(1)))).isEqualTo(TrustLevel.NOVA);
    }

    @Test
    void poucoAntesDeTrintaDiasAindaNaoEConfiavel() {
        historico(5, 0);

        assertThat(calculator.levelOf(contaCom(Duration.ofDays(30).minusMinutes(1)))).isEqualTo(TrustLevel.REGULAR);
    }

    @Test
    void comTrintaDiasEConfiavel() {
        historico(5, 0);

        assertThat(calculator.levelOf(contaCom(Duration.ofDays(30)))).isEqualTo(TrustLevel.CONFIAVEL);
    }

    @Test
    void quatroReviewsNaoBastamParaSerConfiavel() {
        historico(4, 0);

        assertThat(calculator.levelOf(contaCom(Duration.ofDays(40)))).isEqualTo(TrustLevel.REGULAR);
    }

    @Test
    void umaRemocaoNaoDeixaRestritaMasImpedeSerConfiavel() {
        historico(10, 1);

        assertThat(calculator.levelOf(contaCom(Duration.ofDays(40)))).isEqualTo(TrustLevel.REGULAR);
    }

    // ---------- prioridade e casos-limite ----------

    @Test
    void restritaVemAntesDeNova() {
        historico(0, 2);

        assertThat(calculator.levelOf(contaCom(Duration.ofDays(1)))).isEqualTo(TrustLevel.RESTRITA);
    }

    @Test
    void novaVemAntesDeConfiavel() {
        historico(5, 0);

        assertThat(calculator.levelOf(contaCom(Duration.ofDays(1)))).isEqualTo(TrustLevel.NOVA);
    }

    @Test
    void contaSemDataDeCriacaoERegular() {
        historico(10, 0);

        assertThat(calculator.levelOf(User.builder().id("u1").build())).isEqualTo(TrustLevel.REGULAR);
    }

    @Test
    void contaSoAsRemocoesDosUltimosNoventaDias() {
        historico(0, 0);

        calculator.levelOf(contaCom(Duration.ofDays(40)));

        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(reviewRepository).countByUserIdAndModerationRemovedAtAfter(eq("u1"), desde.capture());
        assertThat(desde.getValue()).isCloseTo(LocalDateTime.now().minusDays(90), within(5, ChronoUnit.SECONDS));
    }

    // ---------- peso ----------

    @Test
    void cadaNivelTemOSeuPeso() {
        assertThat(calculator.weightOf(TrustLevel.RESTRITA)).isEqualTo(0.25);
        assertThat(calculator.weightOf(TrustLevel.NOVA)).isEqualTo(0.5);
        assertThat(calculator.weightOf(TrustLevel.REGULAR)).isEqualTo(1.0);
        assertThat(calculator.weightOf(TrustLevel.CONFIAVEL)).isEqualTo(1.5);
    }
}
