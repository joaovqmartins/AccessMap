package br.com.accessmap.backend.review;

import br.com.accessmap.backend.AbstractIntegrationTest;
import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.enums.SuspicionReason;
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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewSuspicionIntegrationTest extends AbstractIntegrationTest {

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

    private User criarUsuario(LocalDateTime criadoEm) {
        return userRepository.save(User.builder()
                .name("Pessoa")
                .phone(telefoneUnico())
                .password(passwordEncoder.encode("senhaForte123"))
                .role(Role.USER)
                .accessibilityNeeds(Set.of())
                .createdAt(criadoEm)
                .updatedAt(criadoEm)
                .build());
    }

    private User contaAntiga() {
        return criarUsuario(LocalDateTime.now().minusDays(40));
    }

    private User contaNova() {
        return criarUsuario(LocalDateTime.now());
    }

    private MockHttpServletRequestBuilder como(User user, MockHttpServletRequestBuilder requisicao) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.issueAccessToken(user));
    }

    private String placeUnico() {
        return "ChIJ-" + UUID.randomUUID();
    }

    private ResultActions enviarAvaliacao(User autor, String placeId, int nota) throws Exception {
        return mockMvc.perform(como(autor, post("/api/reviews"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "placeId", placeId,
                        "rating", nota,
                        "comment", "Teste de suspeita.",
                        "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")))));
    }

    private String avaliar(User autor, String placeId, int nota) throws Exception {
        String corpo = enviarAvaliacao(autor, placeId, nota)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.suspicionReason").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asString();
    }

    private ResultActions denunciar(String reviewId, User denunciante) throws Exception {
        return mockMvc.perform(como(denunciante, post("/api/reviews/" + reviewId + "/report"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", "SPAM"))));
    }

    private void denunciasDeContasAntigas(String reviewId, int total) throws Exception {
        for (int i = 0; i < total; i++) {
            denunciar(reviewId, contaAntiga()).andExpect(status().isCreated());
        }
    }

    /** Local com 5 reviews de nota 5, de contas diferentes. */
    private String localComCincoReviews() throws Exception {
        String placeId = placeUnico();
        for (int i = 0; i < 5; i++) {
            avaliar(contaAntiga(), placeId, 5);
        }
        return placeId;
    }

    private Review review(String id) {
        return reviewRepository.findById(id).orElseThrow();
    }

    // ---------- denúncias ----------

    @Test
    void duasDenunciasNaoOcultam() throws Exception {
        String reviewId = avaliar(contaAntiga(), placeUnico(), 4);

        denunciasDeContasAntigas(reviewId, 2);

        assertThat(review(reviewId).getStatus()).isEqualTo(ReviewStatus.PUBLICADA);
        assertThat(review(reviewId).getSuspicionReason()).isNull();
    }

    @Test
    void tresDenunciasOcultamERecalculamANotaDoLocal() throws Exception {
        String placeId = placeUnico();
        String reviewId = avaliar(contaAntiga(), placeId, 5);
        avaliar(contaAntiga(), placeId, 1);

        denunciasDeContasAntigas(reviewId, 3);

        assertThat(review(reviewId).getStatus()).isEqualTo(ReviewStatus.OCULTA);
        assertThat(review(reviewId).getSuspicionReason()).isEqualTo(SuspicionReason.DENUNCIAS);
        mockMvc.perform(get("/api/places/" + placeId))
                .andExpect(jsonPath("$.averageScore").value(1.0))
                .andExpect(jsonPath("$.reviewCount").value(1));
    }

    @Test
    void reviewOcultaSaiDasLeiturasPublicas() throws Exception {
        String placeId = placeUnico();
        String reviewId = avaliar(contaAntiga(), placeId, 4);

        denunciasDeContasAntigas(reviewId, 3);

        mockMvc.perform(get("/api/reviews/" + reviewId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/reviews").param("placeId", placeId))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void denunciasDeContasNovasFicamRegistradasMasNaoOcultam() throws Exception {
        String reviewId = avaliar(contaAntiga(), placeUnico(), 4);

        for (int i = 0; i < 3; i++) {
            denunciar(reviewId, contaNova()).andExpect(status().isCreated());
        }

        assertThat(review(reviewId).getStatus()).isEqualTo(ReviewStatus.PUBLICADA);
    }

    @Test
    void contaNovaQueEnvelheceDepoisNaoPassaAValer() throws Exception {
        String reviewId = avaliar(contaAntiga(), placeUnico(), 4);
        User denunciante = contaNova();
        denunciar(reviewId, denunciante).andExpect(status().isCreated());

        denunciante.setCreatedAt(LocalDateTime.now().minusDays(40));
        userRepository.save(denunciante);
        denunciasDeContasAntigas(reviewId, 2);

        assertThat(review(reviewId).getStatus()).isEqualTo(ReviewStatus.PUBLICADA);
    }

    @Test
    void reviewRestauradaNaoVoltaASerOcultada() throws Exception {
        String reviewId = avaliar(contaAntiga(), placeUnico(), 4);
        Review restaurada = review(reviewId);
        restaurada.setModerationRestoredAt(LocalDateTime.now());
        reviewRepository.save(restaurada);

        denunciasDeContasAntigas(reviewId, 3);

        assertThat(review(reviewId).getStatus()).isEqualTo(ReviewStatus.PUBLICADA);
    }

    @Test
    void reviewOcultaNaoRecebeNovasDenuncias() throws Exception {
        String reviewId = avaliar(contaAntiga(), placeUnico(), 4);
        denunciasDeContasAntigas(reviewId, 3);

        denunciar(reviewId, contaAntiga()).andExpect(status().isNotFound());
    }

    @Test
    void autorComReviewOcultaRecebe409AoAvaliarDeNovo() throws Exception {
        User autor = contaAntiga();
        String placeId = placeUnico();
        denunciasDeContasAntigas(avaliar(autor, placeId, 4), 3);

        enviarAvaliacao(autor, placeId, 5)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Sua avaliação deste local está em análise."));
    }

    // ---------- nota destoante ----------

    @Test
    void notaDestoanteDeContaNovaContinuaPublicadaENaFila() throws Exception {
        String placeId = localComCincoReviews();

        String reviewId = avaliar(contaNova(), placeId, 1);

        assertThat(review(reviewId).getStatus()).isEqualTo(ReviewStatus.PUBLICADA);
        assertThat(review(reviewId).getSuspicionReason()).isEqualTo(SuspicionReason.NOTA_DESTOANTE);
        mockMvc.perform(get("/api/places/" + placeId)).andExpect(jsonPath("$.reviewCount").value(6));
    }

    @Test
    void notaProximaDaMediaNaoSinaliza() throws Exception {
        String reviewId = avaliar(contaNova(), localComCincoReviews(), 3);

        assertThat(review(reviewId).getSuspicionReason()).isNull();
    }

    @Test
    void contaAntigaNaoESinalizadaPorNotaDestoante() throws Exception {
        String reviewId = avaliar(contaAntiga(), localComCincoReviews(), 1);

        assertThat(review(reviewId).getSuspicionReason()).isNull();
    }

    @Test
    void localComPoucasReviewsNaoSinaliza() throws Exception {
        String placeId = placeUnico();
        for (int i = 0; i < 4; i++) {
            avaliar(contaAntiga(), placeId, 5);
        }

        assertThat(review(avaliar(contaNova(), placeId, 1)).getSuspicionReason()).isNull();
    }

    @Test
    void editarANotaParaDestoanteSinaliza() throws Exception {
        User autor = contaNova();
        String reviewId = avaliar(autor, localComCincoReviews(), 5);
        assertThat(review(reviewId).getSuspicionReason()).isNull();

        mockMvc.perform(como(autor, patch("/api/reviews/" + reviewId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rating", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suspicionReason").doesNotExist());

        assertThat(review(reviewId).getSuspicionReason()).isEqualTo(SuspicionReason.NOTA_DESTOANTE);
    }

    @Test
    void editarSoOComentarioNaoSinaliza() throws Exception {
        User autor = contaNova();
        String reviewId = avaliar(autor, localComCincoReviews(), 5);

        mockMvc.perform(como(autor, patch("/api/reviews/" + reviewId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comment", "Outro texto."))))
                .andExpect(status().isOk());

        assertThat(review(reviewId).getSuspicionReason()).isNull();
    }
}
