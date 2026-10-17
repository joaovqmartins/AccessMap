package br.com.accessmap.backend.place.service;

import br.com.accessmap.backend.identity.enums.AccessibilityNeed;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static br.com.accessmap.backend.review.enums.AccessibilityTag.ATENDIMENTO;
import static br.com.accessmap.backend.review.enums.AccessibilityTag.BANHEIROS_ADAPTADOS;
import static br.com.accessmap.backend.review.enums.AccessibilityTag.RAMPAS_E_ENTRADAS;
import static br.com.accessmap.backend.review.enums.AccessibilityTag.SINALIZACAO;
import static org.assertj.core.api.Assertions.assertThat;

class NeedTagMappingTest {

    @Test
    void mobilidadeReduzidaExigeRampasEBanheiros() {
        assertThat(NeedTagMapping.tagsFor(Set.of(AccessibilityNeed.MOBILIDADE_REDUZIDA)))
                .containsExactlyInAnyOrder(RAMPAS_E_ENTRADAS, BANHEIROS_ADAPTADOS);
    }

    @Test
    void deficienciaVisualExigeSinalizacaoEAtendimento() {
        assertThat(NeedTagMapping.tagsFor(Set.of(AccessibilityNeed.DEFICIENCIA_VISUAL)))
                .containsExactlyInAnyOrder(SINALIZACAO, ATENDIMENTO);
    }

    @Test
    void deficienciaAuditivaExigeSinalizacaoEAtendimento() {
        assertThat(NeedTagMapping.tagsFor(Set.of(AccessibilityNeed.DEFICIENCIA_AUDITIVA)))
                .containsExactlyInAnyOrder(SINALIZACAO, ATENDIMENTO);
    }

    @Test
    void outrosNaoViraFiltro() {
        assertThat(NeedTagMapping.tagsFor(Set.of(AccessibilityNeed.OUTROS))).isEmpty();
    }

    @Test
    void variasNecessidadesSomamAsTagsSemRepetir() {
        assertThat(NeedTagMapping.tagsFor(Set.of(
                AccessibilityNeed.MOBILIDADE_REDUZIDA,
                AccessibilityNeed.DEFICIENCIA_VISUAL,
                AccessibilityNeed.DEFICIENCIA_AUDITIVA,
                AccessibilityNeed.OUTROS)))
                .containsExactlyInAnyOrder(RAMPAS_E_ENTRADAS, BANHEIROS_ADAPTADOS, SINALIZACAO, ATENDIMENTO);
    }

    @Test
    void semNecessidadesNaoFiltra() {
        assertThat(NeedTagMapping.tagsFor(Set.of())).isEmpty();
        assertThat(NeedTagMapping.tagsFor(null)).isEmpty();
    }

}
