package br.com.accessmap.backend.review;

import br.com.accessmap.backend.AbstractIntegrationTest;
import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewAccessIntegrationTest extends AbstractIntegrationTest {

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

    private String json(Map<String, ?> body) {
        return objectMapper.writeValueAsString(body);
    }

    private User criarUsuario(Role role) {
        return userRepository.save(User.builder()
                .name("Pessoa")
                .email("user-" + UUID.randomUUID() + "@teste.com")
                .password(passwordEncoder.encode("senhaForte123"))
                .role(role)
                .accessibilityNeeds(Set.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + tokenService.issueAccessToken(user);
    }

    private Map<String, Object> reviewValida(String placeId) {
        return Map.of(
                "placeId", placeId,
                "rating", 4,
                "comment", "Rampa boa.",
                "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")
        );
    }

    private JsonNode criarReview(User autor, String placeId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, bearer(autor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(reviewValida(placeId))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String placeUnico() {
        return "ChIJ-" + UUID.randomUUID();
    }

    // ---------- autoria ----------

    @Test
    void criarReviewExigeAutenticacao() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(reviewValida(placeUnico()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void autorDaReviewVemDoTokenENaoDoCorpo() throws Exception {
        User autor = criarUsuario(Role.USER);
        User outro = criarUsuario(Role.USER);

        // mesmo enviando userId de outra pessoa no corpo, o autor é quem está autenticado
        Map<String, Object> corpo = new java.util.HashMap<>(reviewValida(placeUnico()));
        corpo.put("userId", outro.getId());

        mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, bearer(autor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(corpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(autor.getId()));
    }

    @Test
    void leituraContinuaPublica() throws Exception {
        User autor = criarUsuario(Role.USER);
        String placeId = placeUnico();
        String reviewId = criarReview(autor, placeId).get("id").asString();

        mockMvc.perform(get("/api/reviews/" + reviewId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/reviews").param("placeId", placeId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/places/" + placeId)).andExpect(status().isOk());
    }

    @Test
    void somenteOAutorEditaOuRemove() throws Exception {
        User autor = criarUsuario(Role.USER);
        User intruso = criarUsuario(Role.USER);
        String reviewId = criarReview(autor, placeUnico()).get("id").asString();

        mockMvc.perform(patch("/api/reviews/" + reviewId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(intruso))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("rating", 1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Você não pode alterar a avaliação de outro usuário"));

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(intruso)))
                .andExpect(status().isForbidden());

        // o autor consegue
        mockMvc.perform(patch("/api/reviews/" + reviewId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(autor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("rating", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(2));

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(autor)))
                .andExpect(status().isOk());
    }

    @Test
    void adminPodeRemoverReviewDeQualquerUsuario() throws Exception {
        User autor = criarUsuario(Role.USER);
        User admin = criarUsuario(Role.ADMIN);
        String reviewId = criarReview(autor, placeUnico()).get("id").asString();

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reviews/" + reviewId)).andExpect(status().isNotFound());
    }

    // ---------- unicidade ----------

    @Test
    void segundaReviewNoMesmoLocalDevolve409() throws Exception {
        User autor = criarUsuario(Role.USER);
        String placeId = placeUnico();
        criarReview(autor, placeId);

        mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, bearer(autor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(reviewValida(placeId))))
                .andExpect(status().isConflict());
    }

    @Test
    void indiceParcialImpedeDuplicataMesmoContornandoOService() throws Exception {
        User autor = criarUsuario(Role.USER);
        String placeId = placeUnico();
        criarReview(autor, placeId);

        // grava direto no repositório, pulando a checagem do service: o banco (V4) barra
        Review duplicada = Review.builder()
                .userId(autor.getId())
                .placeId(placeId)
                .rating(5)
                .tags(Map.of())
                .status(ReviewStatus.PUBLICADA)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> reviewRepository.saveAndFlush(duplicada))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aposRemoverPodeAvaliarDeNovo() throws Exception {
        User autor = criarUsuario(Role.USER);
        String placeId = placeUnico();
        String reviewId = criarReview(autor, placeId).get("id").asString();

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(autor)))
                .andExpect(status().isOk());

        // a removida (soft delete) não conta no índice parcial
        criarReview(autor, placeId);
    }
}
