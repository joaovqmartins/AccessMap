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

/** O que o autor vê e pode fazer com a própria review oculta (#33). */
class ReviewHiddenAuthorIntegrationTest extends AbstractIntegrationTest {

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

    private User criarUsuario(Role role) {
        LocalDateTime criadoEm = LocalDateTime.now().minusDays(40);
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

    private MockHttpServletRequestBuilder como(User user, MockHttpServletRequestBuilder requisicao) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.issueAccessToken(user));
    }

    private String placeUnico() {
        return "ChIJ-" + UUID.randomUUID();
    }

    /** Review de verdade, ocultada por 3 denúncias de contas antigas. */
    private String reviewOculta(User autor, String placeId) throws Exception {
        String corpo = mockMvc.perform(como(autor, post("/api/reviews"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placeId", placeId,
                                "rating", 5,
                                "comment", "Review que vai ser ocultada.",
                                "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reviewId = objectMapper.readTree(corpo).get("id").asString();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(como(criarUsuario(Role.USER), post("/api/reviews/" + reviewId + "/report"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("reason", "SPAM"))))
                    .andExpect(status().isCreated());
        }
        assertThat(review(reviewId).getStatus()).isEqualTo(ReviewStatus.OCULTA);
        return reviewId;
    }

    private Review review(String id) {
        return reviewRepository.findById(id).orElseThrow();
    }

    // ---------- leitura ----------

    @Test
    void oAutorVeAPropriaReviewOculta() throws Exception {
        User autor = criarUsuario(Role.USER);
        String reviewId = reviewOculta(autor, placeUnico());

        mockMvc.perform(como(autor, get("/api/reviews/" + reviewId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OCULTA"))
                .andExpect(jsonPath("$.mine").value(true));
    }

    @Test
    void outrasPessoasContinuamRecebendo404() throws Exception {
        String reviewId = reviewOculta(criarUsuario(Role.USER), placeUnico());

        mockMvc.perform(get("/api/reviews/" + reviewId)).andExpect(status().isNotFound());
        mockMvc.perform(como(criarUsuario(Role.USER), get("/api/reviews/" + reviewId))).andExpect(status().isNotFound());
        mockMvc.perform(como(criarUsuario(Role.ADMIN), get("/api/reviews/" + reviewId))).andExpect(status().isNotFound());
    }

    @Test
    void aListaDoProprioAutorIncluiAOculta() throws Exception {
        User autor = criarUsuario(Role.USER);
        String placeId = placeUnico();
        String reviewId = reviewOculta(autor, placeId);

        mockMvc.perform(como(autor, get("/api/reviews")).param("userId", autor.getId()).param("placeId", placeId))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(reviewId));
        mockMvc.perform(como(autor, get("/api/reviews")).param("userId", autor.getId()))
                .andExpect(jsonPath("$.content[?(@.id == '" + reviewId + "')]").exists());
    }

    @Test
    void aListaPublicaDoLocalEADoAdminNaoIncluem() throws Exception {
        User autor = criarUsuario(Role.USER);
        String placeId = placeUnico();
        reviewOculta(autor, placeId);

        mockMvc.perform(get("/api/reviews").param("placeId", placeId))
                .andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(como(criarUsuario(Role.ADMIN), get("/api/reviews")).param("userId", autor.getId()))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ---------- edição e exclusão ----------

    @Test
    void editarAReviewOcultaDevolve409() throws Exception {
        User autor = criarUsuario(Role.USER);
        String reviewId = reviewOculta(autor, placeUnico());

        mockMvc.perform(como(autor, patch("/api/reviews/" + reviewId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rating", 4))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Sua avaliação está em análise e não pode ser editada."));

        assertThat(review(reviewId).getRating()).isEqualTo(5);
    }

    @Test
    void excluirAReviewOcultaContaComoModeracaoESaiDaFila() throws Exception {
        User autor = criarUsuario(Role.USER);
        String reviewId = reviewOculta(autor, placeUnico());

        mockMvc.perform(como(autor, delete("/api/reviews/" + reviewId))).andExpect(status().isOk());

        Review excluida = review(reviewId);
        assertThat(excluida.getStatus()).isEqualTo(ReviewStatus.REMOVIDA);
        assertThat(excluida.getModerationRemovedAt()).isNotNull();
        assertThat(excluida.getSuspicionReason()).isNull();
    }

    @Test
    void excluirAPropriaReviewPublicadaContinuaSemContarComoModeracao() throws Exception {
        User autor = criarUsuario(Role.USER);
        Review publicada = reviewRepository.save(Review.builder()
                .userId(autor.getId())
                .placeId(placeUnico())
                .rating(4)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build());

        mockMvc.perform(como(autor, delete("/api/reviews/" + publicada.getId()))).andExpect(status().isOk());

        assertThat(review(publicada.getId()).getModerationRemovedAt()).isNull();
    }

    @Test
    void depoisDeExcluirPodeAvaliarDeNovoMasDuasVezesDeixaRestrita() throws Exception {
        User autor = criarUsuario(Role.USER);
        for (int i = 0; i < 2; i++) {
            String reviewId = reviewOculta(autor, placeUnico());
            mockMvc.perform(como(autor, delete("/api/reviews/" + reviewId))).andExpect(status().isOk());
        }

        String corpo = mockMvc.perform(como(autor, post("/api/reviews"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placeId", placeUnico(),
                                "rating", 4,
                                "comment", "Depois de excluir.",
                                "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(review(objectMapper.readTree(corpo).get("id").asString()).getAuthorTrustLevel())
                .isEqualTo(TrustLevel.RESTRITA);
    }

    @Test
    void outraPessoaNaoExcluiNemEditaAReviewOculta() throws Exception {
        String reviewId = reviewOculta(criarUsuario(Role.USER), placeUnico());
        User outra = criarUsuario(Role.USER);

        mockMvc.perform(como(outra, delete("/api/reviews/" + reviewId))).andExpect(status().isNotFound());
        mockMvc.perform(como(outra, patch("/api/reviews/" + reviewId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rating", 1))))
                .andExpect(status().isNotFound());

        assertThat(review(reviewId).getStatus()).isEqualTo(ReviewStatus.OCULTA);
    }
}
