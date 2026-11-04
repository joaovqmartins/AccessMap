package br.com.accessmap.backend.review;

import br.com.accessmap.backend.AbstractIntegrationTest;
import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import br.com.accessmap.backend.review.repository.ReviewReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewReportIntegrationTest extends AbstractIntegrationTest {

    private static final String SPAM = "{\"reason\":\"SPAM\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewReportRepository reviewReportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    // ---------- helpers ----------

    private User criarUsuario() {
        return userRepository.save(User.builder()
                .name("Pessoa")
                .phone(telefoneUnico())
                .password(passwordEncoder.encode("senhaForte123"))
                .role(Role.USER)
                .accessibilityNeeds(Set.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + tokenService.issueAccessToken(user);
    }

    /** Cria uma avaliação de {@code autor} e devolve o id. */
    private String criarReview(User autor, String placeId) throws Exception {
        String corpo = mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, bearer(autor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placeId", placeId,
                                "rating", 4,
                                "comment", "Rampa boa.",
                                "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asString();
    }

    private String placeUnico() {
        return "ChIJ-" + UUID.randomUUID();
    }

    private ResultActions denunciar(String reviewId, User denunciante, String corpo) throws Exception {
        return mockMvc.perform(post("/api/reviews/" + reviewId + "/report")
                .header(HttpHeaders.AUTHORIZATION, bearer(denunciante))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    // ---------- denúncia registrada ----------

    @Test
    void denunciaValidaEhRegistradaSemExporQuemDenunciou() throws Exception {
        String reviewId = criarReview(criarUsuario(), placeUnico());
        User denunciante = criarUsuario();

        String corpo = denunciar(reviewId, denunciante, SPAM)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Denúncia registrada"))
                .andReturn().getResponse().getContentAsString();

        assertThat(reviewReportRepository.existsByReviewIdAndReporterId(reviewId, denunciante.getId())).isTrue();
        assertThat(corpo).doesNotContain(denunciante.getId());
    }

    @Test
    void pessoasDiferentesPodemDenunciarAMesmaAvaliacao() throws Exception {
        String reviewId = criarReview(criarUsuario(), placeUnico());

        denunciar(reviewId, criarUsuario(), SPAM).andExpect(status().isCreated());
        denunciar(reviewId, criarUsuario(), "{\"reason\":\"INFORMACAO_FALSA\"}").andExpect(status().isCreated());

        assertThat(reviewReportRepository.countByReviewId(reviewId)).isEqualTo(2);
    }

    @Test
    void denunciaNaoOcultaAAvaliacaoNemMudaANota() throws Exception {
        String placeId = placeUnico();
        String reviewId = criarReview(criarUsuario(), placeId);

        denunciar(reviewId, criarUsuario(), SPAM).andExpect(status().isCreated());

        mockMvc.perform(get("/api/reviews/" + reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLICADA"));
        mockMvc.perform(get("/api/places/" + placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageScore").value(4.0))
                .andExpect(jsonPath("$.reviewCount").value(1));
    }

    // ---------- bloqueios ----------

    @Test
    void denunciarExigeAutenticacao() throws Exception {
        String reviewId = criarReview(criarUsuario(), placeUnico());

        mockMvc.perform(post("/api/reviews/" + reviewId + "/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SPAM))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void naoPodeDenunciarAPropriaAvaliacao() throws Exception {
        User autor = criarUsuario();
        String reviewId = criarReview(autor, placeUnico());

        denunciar(reviewId, autor, SPAM).andExpect(status().isForbidden());

        assertThat(reviewReportRepository.countByReviewId(reviewId)).isZero();
    }

    @Test
    void denunciaRepetidaDevolve409EContaUmaVezSo() throws Exception {
        String reviewId = criarReview(criarUsuario(), placeUnico());
        User denunciante = criarUsuario();

        denunciar(reviewId, denunciante, SPAM).andExpect(status().isCreated());
        denunciar(reviewId, denunciante, "{\"reason\":\"OUTRO\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Você já denunciou esta avaliação"));

        assertThat(reviewReportRepository.countByReviewId(reviewId)).isEqualTo(1);
    }

    @Test
    void avaliacaoInexistenteDevolve404() throws Exception {
        denunciar(UUID.randomUUID().toString(), criarUsuario(), SPAM).andExpect(status().isNotFound());
    }

    @Test
    void avaliacaoRemovidaDevolve404() throws Exception {
        User autor = criarUsuario();
        String reviewId = criarReview(autor, placeUnico());
        mockMvc.perform(delete("/api/reviews/" + reviewId).header(HttpHeaders.AUTHORIZATION, bearer(autor)))
                .andExpect(status().isOk());

        denunciar(reviewId, criarUsuario(), SPAM).andExpect(status().isNotFound());
    }

    // ---------- validação ----------

    @Test
    void semMotivoDevolve400ApontandoOCampo() throws Exception {
        String reviewId = criarReview(criarUsuario(), placeUnico());

        denunciar(reviewId, criarUsuario(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.reason").exists());
    }

    @Test
    void motivoInexistenteDevolve400ComOsValoresAceitos() throws Exception {
        String reviewId = criarReview(criarUsuario(), placeUnico());

        denunciar(reviewId, criarUsuario(), "{\"reason\":\"BANANA\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("BANANA")))
                .andExpect(jsonPath("$.mensagem").value(containsString("INFORMACAO_FALSA")));
    }

    // ---------- exclusão de conta ----------

    @Test
    void excluirAContaDeQuemDenunciouNaoTravaEApagaADenuncia() throws Exception {
        String reviewId = criarReview(criarUsuario(), placeUnico());
        User denunciante = criarUsuario();
        denunciar(reviewId, denunciante, SPAM).andExpect(status().isCreated());

        userRepository.deleteById(denunciante.getId());

        assertThat(reviewReportRepository.countByReviewId(reviewId)).isZero();
    }
}
