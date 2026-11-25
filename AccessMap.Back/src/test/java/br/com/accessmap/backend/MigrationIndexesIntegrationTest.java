package br.com.accessmap.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os índices da V13 (#131) existem no banco migrado pelo Flyway, com as colunas e o filtro esperados.
 * Falha se alguém renomear, apagar ou mudar um índice que as consultas de avaliações usam.
 */
class MigrationIndexesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private String definition(String indexName) {
        List<String> found = jdbc.queryForList(
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'public' AND indexname = ?",
                String.class, indexName);
        assertThat(found).as("índice %s", indexName).hasSize(1);
        return found.get(0);
    }

    @Test
    void avaliacoesDoLocalPorStatusEDataDecrescente() {
        assertThat(definition("idx_reviews_place_status_created"))
                .contains("ON public.reviews")
                .contains("(place_id, status, created_at DESC)");
    }

    @Test
    void filaDeModeracaoSoComAsReviewsSuspeitas() {
        assertThat(definition("idx_reviews_moderation_queue"))
                .contains("ON public.reviews")
                .contains("(created_at)")
                .contains("WHERE (suspicion_reason IS NOT NULL)");
    }

    @Test
    void denunciasPorQuemDenunciouParaAExclusaoDeConta() {
        assertThat(definition("idx_review_reports_reporter"))
                .contains("ON public.review_reports")
                .contains("(reporter_id)");
    }
}
