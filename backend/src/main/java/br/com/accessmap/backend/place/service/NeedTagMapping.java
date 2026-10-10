package br.com.accessmap.backend.place.service;

import br.com.accessmap.backend.identity.enums.AccessibilityNeed;
import br.com.accessmap.backend.review.enums.AccessibilityTag;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Quais características do local são essenciais para cada necessidade de acessibilidade do usuário.
 * <p>
 * Só as essenciais, de propósito: exigir todas as tags relacionadas (elevador, vaga, circulação...) deixaria
 * quase nenhum local passar enquanto há poucas avaliações. Fica em {@code place} porque é a busca que usa,
 * e assim {@code identity} não precisa conhecer as tags de {@code review}.
 */
public final class NeedTagMapping {

    private static final Map<AccessibilityNeed, Set<AccessibilityTag>> TAGS_POR_NECESSIDADE =
            new EnumMap<>(AccessibilityNeed.class);

    static {
        // entrar no local e usar o banheiro
        TAGS_POR_NECESSIDADE.put(AccessibilityNeed.MOBILIDADE_REDUZIDA,
                EnumSet.of(AccessibilityTag.RAMPAS_E_ENTRADAS, AccessibilityTag.BANHEIROS_ADAPTADOS));
        // piso tátil/braille e atendimento preparado
        TAGS_POR_NECESSIDADE.put(AccessibilityNeed.DEFICIENCIA_VISUAL,
                EnumSet.of(AccessibilityTag.SINALIZACAO, AccessibilityTag.ATENDIMENTO));
        // sinalização visual e atendimento em Libras
        TAGS_POR_NECESSIDADE.put(AccessibilityNeed.DEFICIENCIA_AUDITIVA,
                EnumSet.of(AccessibilityTag.SINALIZACAO, AccessibilityTag.ATENDIMENTO));
        // genérico demais para virar filtro
        TAGS_POR_NECESSIDADE.put(AccessibilityNeed.OUTROS, EnumSet.noneOf(AccessibilityTag.class));
    }

    private NeedTagMapping() {
    }

    /** União das tags essenciais de todas as necessidades; vazio se não houver necessidade que filtre. */
    public static Set<AccessibilityTag> tagsFor(Collection<AccessibilityNeed> needs) {
        Set<AccessibilityTag> tags = EnumSet.noneOf(AccessibilityTag.class);
        if (needs != null) {
            needs.forEach(need -> tags.addAll(TAGS_POR_NECESSIDADE.getOrDefault(need, Set.of())));
        }
        return tags;
    }
}
