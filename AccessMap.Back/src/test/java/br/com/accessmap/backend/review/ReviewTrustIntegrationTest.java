package br.com.accessmap.backend.review;

import br.com.accessmap.backend.AbstractIntegrationTest;
import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.enums.TrustLevel;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewTrustIntegrationTest extends AbstractIntegrationTest {

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
                .accessibilityNeeds(Set.of())
                .createdAt(criadoEm)
                .updatedAt(criadoEm)
                .build());
    }

    private User contaAntiga() {
        return criarUsuario(Role.USER, LocalDateTime.now().minusDays(40));
    }

    private MockHttpServletRequestBuilder como(User user, MockHttpServletRequestBuilder requisicao) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.issueAccessToken(user));
    }

    private String placeUnico() {
        return "ChIJ-" + UUID.randomUUID();
    }

    /** Publica pela API e devolve o id: é o caminho que grava o nível. */
    private String avaliar(User autor) throws Exception {
        String corpo = mockMvc.perform(como(autor, post("/api/reviews"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placeId", placeUnico(),
                                "rating", 4,
                                "comment", "Teste de confiabilidade.",
                                "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorTrustLevel").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asString();
    }

    private TrustLevel nivelDaReview(String reviewId) {
        return reviewRepository.findById(reviewId).orElseThrow().getAuthorTrustLevel();
    }

    /** Grava direto no banco, com data antiga, para montar histórico sem esbarrar no limite da #100. */
    private Review historico(User autor, ReviewStatus status, LocalDateTime removidaEm) {
        LocalDateTime criadaEm = LocalDateTime.now().minusDays(20);
        return reviewRepository.save(Review.builder()
                .userId(autor.getId())
                .placeId(placeUnico())
                .rating(4)
                .status(status)
                .moderationRemovedAt(removidaEm)
                .createdAt(criadaEm)
                .updatedAt(criadaEm)
                .build());
    }

    // ---------- nível gravado na review ----------

    @Test
    void contaNovaGravaNova() throws Exception {
        String reviewId = avaliar(criarUsuario(Role.USER, LocalDateTime.now()));

        assertThat(nivelDaReview(reviewId)).isEqualTo(TrustLevel.NOVA);
    }

    @Test
    void contaAntigaSemHistoricoGravaRegular() throws Exception {
        assertThat(nivelDaReview(avaliar(contaAntiga()))).isEqualTo(TrustLevel.REGULAR);
    }

    @Test
    void contaComCincoReviewsPublicadasGravaConfiavel() throws Exception {
        User autor = contaAntiga();
        for (int i = 0; i < 5; i++) {
            historico(autor, ReviewStatus.PUBLICADA, null);
        }

        assertThat(nivelDaReview(avaliar(autor))).isEqualTo(TrustLevel.CONFIAVEL);
    }

    @Test
    void reviewsRemovidasNaoContamParaSerConfiavel() throws Exception {
        User autor = contaAntiga();
        for (int i = 0; i < 4; i++) {
            historico(autor, ReviewStatus.PUBLICADA, null);
        }
        historico(autor, ReviewStatus.REMOVIDA, null);

        assertThat(nivelDaReview(avaliar(autor))).isEqualTo(TrustLevel.REGULAR);
    }

    @Test
    void oNivelGravadoNaoMudaDepois() throws Exception {
        User autor = criarUsuario(Role.USER, LocalDateTime.now());
        String reviewId = avaliar(autor);

        autor.setCreatedAt(LocalDateTime.now().minusDays(40));
        userRepository.save(autor);
        mockMvc.perform(como(autor, patch("/api/reviews/" + reviewId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rating", 2))))
                .andExpect(status().isOk());

        assertThat(nivelDaReview(reviewId)).isEqualTo(TrustLevel.NOVA);
    }

    // ---------- selo na resposta ----------

    private String reviewComNivel(TrustLevel nivel) {
        Review review = historico(contaAntiga(), ReviewStatus.PUBLICADA, null);
        review.setAuthorTrustLevel(nivel);
        return reviewRepository.save(review).getId();
    }

    @Test
    void contaConfiavelTemSelo() throws Exception {
        mockMvc.perform(get("/api/reviews/" + reviewComNivel(TrustLevel.CONFIAVEL)))
                .andExpect(jsonPath("$.trustBadge").value("CONFIAVEL"));
    }

    @Test
    void contaNovaTemSelo() throws Exception {
        mockMvc.perform(get("/api/reviews/" + reviewComNivel(TrustLevel.NOVA)))
                .andExpect(jsonPath("$.trustBadge").value("NOVA"));
    }

    @Test
    void contaRegularNaoTemSelo() throws Exception {
        mockMvc.perform(get("/api/reviews/" + reviewComNivel(TrustLevel.REGULAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustBadge").doesNotExist());
    }

    @Test
    void contaRestritaNaoApareceNaResposta() throws Exception {
        String corpo = mockMvc.perform(get("/api/reviews/" + reviewComNivel(TrustLevel.RESTRITA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustBadge").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).doesNotContain("RESTRITA");
    }

    @Test
    void aListagemTambemTrazOSelo() throws Exception {
        String reviewId = reviewComNivel(TrustLevel.CONFIAVEL);
        String placeId = reviewRepository.findById(reviewId).orElseThrow().getPlaceId();

        mockMvc.perform(get("/api/reviews").param("placeId", placeId))
                .andExpect(jsonPath("$.content[0].trustBadge").value("CONFIAVEL"));
    }

    // ---------- remoção pela moderação ----------

    @Test
    void adminExcluirReviewDeOutroContaComoModeracao() throws Exception {
        User autor = contaAntiga();
        Review alvo = historico(autor, ReviewStatus.PUBLICADA, null);

        mockMvc.perform(como(criarUsuario(Role.ADMIN, LocalDateTime.now()), delete("/api/reviews/" + alvo.getId())))
                .andExpect(status().isOk());

        assertThat(reviewRepository.findById(alvo.getId()).orElseThrow().getModerationRemovedAt()).isNotNull();
    }

    @Test
    void autorExcluirAPropriaNaoContaComoModeracao() throws Exception {
        User autor = contaAntiga();
        Review alvo = historico(autor, ReviewStatus.PUBLICADA, null);

        mockMvc.perform(como(autor, delete("/api/reviews/" + alvo.getId()))).andExpect(status().isOk());

        Review removida = reviewRepository.findById(alvo.getId()).orElseThrow();
        assertThat(removida.getStatus()).isEqualTo(ReviewStatus.REMOVIDA);
        assertThat(removida.getModerationRemovedAt()).isNull();
    }

    @Test
    void duasRemocoesDaModeracaoGravamRestrita() throws Exception {
        User autor = contaAntiga();
        User admin = criarUsuario(Role.ADMIN, LocalDateTime.now());
        for (int i = 0; i < 2; i++) {
            Review alvo = historico(autor, ReviewStatus.PUBLICADA, null);
            mockMvc.perform(como(admin, delete("/api/reviews/" + alvo.getId()))).andExpect(status().isOk());
        }

        assertThat(nivelDaReview(avaliar(autor))).isEqualTo(TrustLevel.RESTRITA);
    }

    @Test
    void remocoesForaDosNoventaDiasNaoContam() throws Exception {
        User autor = contaAntiga();
        historico(autor, ReviewStatus.REMOVIDA, LocalDateTime.now().minusDays(91));
        historico(autor, ReviewStatus.REMOVIDA, LocalDateTime.now().minusDays(91));

        assertThat(nivelDaReview(avaliar(autor))).isEqualTo(TrustLevel.REGULAR);
    }
}
