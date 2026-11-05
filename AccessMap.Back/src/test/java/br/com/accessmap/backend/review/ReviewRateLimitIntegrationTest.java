package br.com.accessmap.backend.review;

import br.com.accessmap.backend.AbstractIntegrationTest;
import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Limites baixos só nesta classe, para não depender dos valores padrão nem afetar os outros testes. */
@TestPropertySource(properties = {
        "accessmap.review.rate-limit.per-hour=3",
        "accessmap.review.rate-limit.per-day=4",
        "accessmap.review.rate-limit.new-account-per-hour=1",
        "accessmap.review.rate-limit.new-account-per-day=2",
        "accessmap.review.rate-limit.new-account-period=7d"
})
class ReviewRateLimitIntegrationTest extends AbstractIntegrationTest {

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
        return criarUsuario(Role.USER, LocalDateTime.now().minusDays(30));
    }

    private User contaNova() {
        return criarUsuario(Role.USER, LocalDateTime.now());
    }

    private String bearer(User user) {
        return "Bearer " + tokenService.issueAccessToken(user);
    }

    private String placeUnico() {
        return "ChIJ-" + UUID.randomUUID();
    }

    private ResultActions avaliar(User autor, String placeId) throws Exception {
        return mockMvc.perform(post("/api/reviews")
                .header(HttpHeaders.AUTHORIZATION, bearer(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "placeId", placeId,
                        "rating", 4,
                        "comment", "Teste de limite.",
                        "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")))));
    }

    private ResultActions avaliar(User autor) throws Exception {
        return avaliar(autor, placeUnico());
    }

    private String idDaReview(ResultActions resultado) throws Exception {
        String corpo = resultado.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asString();
    }

    /** Grava uma avaliação direto no banco, com a data informada, para simular o passado. */
    private void avaliacaoAntiga(User autor, LocalDateTime criadaEm) {
        reviewRepository.save(Review.builder()
                .userId(autor.getId())
                .placeId(placeUnico())
                .rating(4)
                .createdAt(criadaEm)
                .updatedAt(criadaEm)
                .build());
    }

    // ---------- limites ----------

    @Test
    void dentroDoLimitePassaEAcimaDevolve429ComMensagemClara() throws Exception {
        User autor = contaAntiga();

        avaliar(autor).andExpect(status().isCreated());
        avaliar(autor).andExpect(status().isCreated());
        avaliar(autor).andExpect(status().isCreated());

        avaliar(autor)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.mensagem").value(containsString("Tente novamente mais tarde")));
    }

    @Test
    void limitePorDiaConsideraAvaliacoesForaDaUltimaHora() throws Exception {
        User autor = contaAntiga();
        for (int i = 0; i < 4; i++) {
            avaliacaoAntiga(autor, LocalDateTime.now().minusHours(5));
        }

        avaliar(autor).andExpect(status().isTooManyRequests());
    }

    @Test
    void avaliacoesForaDasJanelasNaoContam() throws Exception {
        User autor = contaAntiga();
        for (int i = 0; i < 4; i++) {
            avaliacaoAntiga(autor, LocalDateTime.now().minusDays(2));
        }

        avaliar(autor).andExpect(status().isCreated());
    }

    @Test
    void contaNovaTemLimiteMenor() throws Exception {
        User autor = contaNova();

        avaliar(autor).andExpect(status().isCreated());
        avaliar(autor).andExpect(status().isTooManyRequests());
    }

    @Test
    void adminNaoTemLimite() throws Exception {
        User admin = criarUsuario(Role.ADMIN, LocalDateTime.now());

        for (int i = 0; i < 5; i++) {
            avaliar(admin).andExpect(status().isCreated());
        }
    }

    @Test
    void limiteDeUmUsuarioNaoAfetaOutro() throws Exception {
        User bloqueado = contaNova();
        avaliar(bloqueado).andExpect(status().isCreated());
        avaliar(bloqueado).andExpect(status().isTooManyRequests());

        avaliar(contaNova()).andExpect(status().isCreated());
    }

    // ---------- o que conta e o que não conta ----------

    @Test
    void editarAPropriaAvaliacaoNaoConta() throws Exception {
        User autor = contaNova();
        String reviewId = idDaReview(avaliar(autor));

        for (int nota = 1; nota <= 3; nota++) {
            mockMvc.perform(patch("/api/reviews/" + reviewId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(autor))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rating\":" + nota + "}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void excluirERecriarConta() throws Exception {
        User autor = contaNova();
        String placeId = placeUnico();
        String reviewId = idDaReview(avaliar(autor, placeId));

        mockMvc.perform(delete("/api/reviews/" + reviewId).header(HttpHeaders.AUTHORIZATION, bearer(autor)))
                .andExpect(status().isOk());

        avaliar(autor, placeId).andExpect(status().isTooManyRequests());
    }

    @Test
    void localJaAvaliadoContinuaDevolvendo409MesmoNoLimite() throws Exception {
        User autor = contaNova();
        String placeId = placeUnico();
        avaliar(autor, placeId).andExpect(status().isCreated());

        avaliar(autor, placeId).andExpect(status().isConflict());
    }
}
