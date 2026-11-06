package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.review.config.ReviewRateLimitProperties;
import br.com.accessmap.backend.review.config.ReviewSuspicionProperties;
import br.com.accessmap.backend.review.enums.TrustLevel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewSuspicionRulesTest {

    private final ReviewSuspicionRules rules = new ReviewSuspicionRules(
            new ReviewSuspicionProperties(3, 3, 5),
            new ReviewRateLimitProperties(5, 20, 3, 10, Duration.ofDays(7)));

    private User contaCom(Duration idade) {
        return User.builder().createdAt(LocalDateTime.now().minus(idade)).build();
    }

    @Test
    void contaComSeteDiasOuMaisEEstabelecida() {
        assertThat(rules.isEstablished(contaCom(Duration.ofDays(7)))).isTrue();
    }

    @Test
    void contaComMenosDeSeteDiasNaoEEstabelecida() {
        assertThat(rules.isEstablished(contaCom(Duration.ofDays(7).minusMinutes(1)))).isFalse();
    }

    @Test
    void contaSemDataNaoEEstabelecida() {
        assertThat(rules.isEstablished(User.builder().build())).isFalse();
    }

    @Test
    void limiteDeDenunciasEDeTres() {
        assertThat(rules.reachedReportLimit(2)).isFalse();
        assertThat(rules.reachedReportLimit(3)).isTrue();
    }

    @Test
    void contaNovaComNotaLongeDaMediaEDestoante() {
        assertThat(rules.isRatingOutlier(TrustLevel.NOVA, 1, 5, 4.5)).isTrue();
    }

    @Test
    void notaAltaLongeDaMediaTambemEDestoante() {
        assertThat(rules.isRatingOutlier(TrustLevel.NOVA, 5, 5, 1.5)).isTrue();
    }

    @Test
    void diferencaDeExatamenteTresPontosEDestoante() {
        assertThat(rules.isRatingOutlier(TrustLevel.NOVA, 2, 5, 5.0)).isTrue();
    }

    @Test
    void diferencaMenorQueTresPontosNaoEDestoante() {
        assertThat(rules.isRatingOutlier(TrustLevel.NOVA, 3, 5, 5.0)).isFalse();
    }

    @Test
    void localComMenosDeCincoReviewsNaoSinaliza() {
        assertThat(rules.isRatingOutlier(TrustLevel.NOVA, 1, 4, 5.0)).isFalse();
    }

    @Test
    void soContaNovaESinalizada() {
        assertThat(rules.isRatingOutlier(TrustLevel.REGULAR, 1, 5, 5.0)).isFalse();
        assertThat(rules.isRatingOutlier(TrustLevel.CONFIAVEL, 1, 5, 5.0)).isFalse();
        assertThat(rules.isRatingOutlier(TrustLevel.RESTRITA, 1, 5, 5.0)).isFalse();
    }
}
