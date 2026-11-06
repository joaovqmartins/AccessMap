package br.com.accessmap.backend.review;

import br.com.accessmap.backend.AbstractIntegrationTest;
import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.enums.AccessibilityNeed;
import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.enums.SuspicionReason;
import br.com.accessmap.backend.review.enums.TrustLevel;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewModerationIntegrationTest extends AbstractIntegrationTest {

    private static final String FILA = "/api/admin/reviews";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    // ---------- helpers ----------

    private User criarUsuario(Role role, LocalDateTime criadoEm) {
        return userRepository.save(User.builder()
                .name("Pessoa")
                .phone(telefoneUnico())
                .password(passwordEncoder.encode("senhaForte123"))
                .role(role)
                .accessibilityNeeds(Set.of(AccessibilityNeed.MOBILIDADE_REDUZIDA))
                .createdAt(criadoEm)
                .updatedAt(criadoEm)
                .build());
    }

    private User contaAntiga() {
        return criarUsuario(Role.USER, LocalDateTime.now().minusDays(40));
    }

    private User admin() {
        return criarUsuario(Role.ADMIN, LocalDateTime.now().minusDays(40));
    }

    private MockHttpServletRequestBuilder como(User user, MockHttpServletRequestBuilder requisicao) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.issueAccessToken(user));
    }

    private String placeUnico() {
        return "ChIJ-" + UUID.randomUUID();
    }

    private String avaliar(User autor, String placeId, int nota) throws Exception {
        String corpo = mockMvc.perform(como(autor, post("/api/reviews"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placeId", placeId,
                                "rating", nota,
                                "comment", "Teste de moderação.",
                                "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asString();
    }

    private void denunciar(String reviewId, int total) throws Exception {
        for (int i = 0; i < total; i++) {
            mockMvc.perform(como(contaAntiga(), post("/api/reviews/" + reviewId + "/report"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("reason", "SPAM"))))
                    .andExpect(status().isCreated());
        }
    }

    /** Review ocultada de verdade, por 3 denúncias, num local onde outra review tem nota 1. */
    private String reviewOculta(String placeId) throws Exception {
        String reviewId = avaliar(contaAntiga(), placeId, 5);
        avaliar(contaAntiga(), placeId, 1);
        denunciar(reviewId, 3);
        return reviewId;
    }

    private Review reviewSinalizadaPublicada() {
        LocalDateTime criadaEm = LocalDateTime.now().minusDays(1);
        return reviewRepository.save(Review.builder()
                .userId(UUID.randomUUID().toString())
                .placeId(placeUnico())
                .rating(1)
                .suspicionReason(SuspicionReason.NOTA_DESTOANTE)
                .authorTrustLevel(TrustLevel.NOVA)
                .createdAt(criadaEm)
                .updatedAt(criadaEm)
                .build());
    }

    private Review review(String id) {
        return reviewRepository.findById(id).orElseThrow();
    }

    private ResultActions acao(User quem, String id, String acao) throws Exception {
        return mockMvc.perform(como(quem, post(FILA + "/" + id + "/" + acao)));
    }

    /** O item da fila com este id, ou {@code null}. O banco é compartilhado, e a fila pode ter outras reviews. */
    private JsonNode itemDaFila(String id) throws Exception {
        String corpo = mockMvc.perform(como(admin(), get(FILA)).param("size", "1000"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode item : objectMapper.readTree(corpo).get("content")) {
            if (item.get("id").asString().equals(id)) {
                return item;
            }
        }
        return null;
    }

    // ---------- acesso ----------

    @Test
    void semLoginDevolve401() throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(get(FILA)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(FILA + "/" + id + "/restore")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(FILA + "/" + id + "/keep")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(FILA + "/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioComumDevolve403() throws Exception {
        User comum = contaAntiga();
        String id = reviewSinalizadaPublicada().getId();

        mockMvc.perform(como(comum, get(FILA))).andExpect(status().isForbidden());
        acao(comum, id, "restore").andExpect(status().isForbidden());
        acao(comum, id, "keep").andExpect(status().isForbidden());
        mockMvc.perform(como(comum, delete(FILA + "/" + id))).andExpect(status().isForbidden());

        assertThat(review(id).getSuspicionReason()).isEqualTo(SuspicionReason.NOTA_DESTOANTE);
    }

    // ---------- fila ----------

    @Test
    void reviewOcultaApareceComMotivoEDenuncias() throws Exception {
        User autor = contaAntiga();
        String reviewId = avaliar(autor, placeUnico(), 5);
        denunciar(reviewId, 3);

        JsonNode item = itemDaFila(reviewId);

        assertThat(item.get("status").asString()).isEqualTo("OCULTA");
        assertThat(item.get("suspicionReason").asString()).isEqualTo("DENUNCIAS");
        assertThat(item.get("reportCount").asLong()).isEqualTo(3);
        assertThat(item.get("userId").asString()).isEqualTo(autor.getId());
    }

    @Test
    void reviewPublicadaSinalizadaApareceNaFila() throws Exception {
        JsonNode item = itemDaFila(reviewSinalizadaPublicada().getId());

        assertThat(item.get("status").asString()).isEqualTo("PUBLICADA");
        assertThat(item.get("suspicionReason").asString()).isEqualTo("NOTA_DESTOANTE");
        assertThat(item.get("authorTrustLevel").asString()).isEqualTo("NOVA");
    }

    @Test
    void reviewComumNaoApareceNaFila() throws Exception {
        assertThat(itemDaFila(avaliar(contaAntiga(), placeUnico(), 4))).isNull();
    }

    @Test
    void filaNaoTrazAsNecessidadesDoAutor() throws Exception {
        String reviewId = avaliar(contaAntiga(), placeUnico(), 5);
        denunciar(reviewId, 3);

        assertThat(review(reviewId).getReviewerNeeds()).isNotEmpty();
        assertThat(itemDaFila(reviewId).has("reviewerNeeds")).isFalse();
    }

    // ---------- restaurar ----------

    @Test
    void restaurarVoltaANotaESaiDaFila() throws Exception {
        String placeId = placeUnico();
        String reviewId = reviewOculta(placeId);
        mockMvc.perform(get("/api/places/" + placeId)).andExpect(jsonPath("$.averageScore").value(1.0));

        acao(admin(), reviewId, "restore").andExpect(status().isNoContent());

        assertThat(review(reviewId).getStatus()).isEqualTo(ReviewStatus.PUBLICADA);
        assertThat(itemDaFila(reviewId)).isNull();
        mockMvc.perform(get("/api/reviews/" + reviewId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/places/" + placeId)).andExpect(jsonPath("$.averageScore").value(3.0));
    }

    @Test
    void reviewRestauradaNaoEOcultadaDeNovoPorDenuncias() throws Exception {
        String reviewId = reviewOculta(placeUnico());
        acao(admin(), reviewId, "restore").andExpect(status().isNoContent());

        denunciar(reviewId, 3);

        assertThat(review(reviewId).getStatus()).isEqualTo(ReviewStatus.PUBLICADA);
    }

    @Test
    void restaurarReviewQueNaoEstaOcultaDevolve409() throws Exception {
        acao(admin(), reviewSinalizadaPublicada().getId(), "restore").andExpect(status().isConflict());
    }

    // ---------- manter ----------

    @Test
    void manterTiraDaFilaEDeixaNoAr() throws Exception {
        String id = reviewSinalizadaPublicada().getId();

        acao(admin(), id, "keep").andExpect(status().isNoContent());

        assertThat(review(id).getStatus()).isEqualTo(ReviewStatus.PUBLICADA);
        assertThat(itemDaFila(id)).isNull();
    }

    @Test
    void reviewMantidaAindaPodeSerOcultadaPorDenuncias() throws Exception {
        String reviewId = avaliar(contaAntiga(), placeUnico(), 4);
        Review sinalizada = review(reviewId);
        sinalizada.setSuspicionReason(SuspicionReason.NOTA_DESTOANTE);
        reviewRepository.save(sinalizada);
        acao(admin(), reviewId, "keep").andExpect(status().isNoContent());

        denunciar(reviewId, 3);

        assertThat(review(reviewId).getStatus()).isEqualTo(ReviewStatus.OCULTA);
    }

    @Test
    void manterReviewOcultaDevolve409() throws Exception {
        acao(admin(), reviewOculta(placeUnico()), "keep").andExpect(status().isConflict());
    }

    @Test
    void manterReviewForaDaFilaDevolve409() throws Exception {
        acao(admin(), avaliar(contaAntiga(), placeUnico(), 4), "keep").andExpect(status().isConflict());
    }

    // ---------- remover ----------

    @Test
    void removerMarcaComoModeracaoESaiDaFila() throws Exception {
        String reviewId = reviewOculta(placeUnico());

        mockMvc.perform(como(admin(), delete(FILA + "/" + reviewId))).andExpect(status().isNoContent());

        assertThat(review(reviewId).getStatus()).isEqualTo(ReviewStatus.REMOVIDA);
        assertThat(review(reviewId).getModerationRemovedAt()).isNotNull();
        assertThat(itemDaFila(reviewId)).isNull();
    }

    @Test
    void removerReviewPublicadaRecalculaANota() throws Exception {
        String placeId = placeUnico();
        String reviewId = avaliar(contaAntiga(), placeId, 5);
        avaliar(contaAntiga(), placeId, 1);

        mockMvc.perform(como(admin(), delete(FILA + "/" + reviewId))).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/places/" + placeId)).andExpect(jsonPath("$.averageScore").value(1.0));
    }

    @Test
    void duasRemocoesDeixamOAutorRestrito() throws Exception {
        User autor = contaAntiga();
        User moderador = admin();
        mockMvc.perform(como(moderador, delete(FILA + "/" + avaliar(autor, placeUnico(), 4)))).andExpect(status().isNoContent());
        mockMvc.perform(como(moderador, delete(FILA + "/" + avaliar(autor, placeUnico(), 4)))).andExpect(status().isNoContent());

        assertThat(review(avaliar(autor, placeUnico(), 4)).getAuthorTrustLevel()).isEqualTo(TrustLevel.RESTRITA);
    }

    @Test
    void removerDuasVezesDevolve404() throws Exception {
        String reviewId = avaliar(contaAntiga(), placeUnico(), 4);
        User moderador = admin();
        mockMvc.perform(como(moderador, delete(FILA + "/" + reviewId))).andExpect(status().isNoContent());

        mockMvc.perform(como(moderador, delete(FILA + "/" + reviewId))).andExpect(status().isNotFound());
    }

    @Test
    void reviewInexistenteDevolve404NasTresAcoes() throws Exception {
        String id = UUID.randomUUID().toString();
        User moderador = admin();

        acao(moderador, id, "restore").andExpect(status().isNotFound());
        acao(moderador, id, "keep").andExpect(status().isNotFound());
        mockMvc.perform(como(moderador, delete(FILA + "/" + id))).andExpect(status().isNotFound());
    }
}
