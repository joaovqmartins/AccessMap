package br.com.accessmap.backend.identity;

import br.com.accessmap.backend.AbstractIntegrationTest;
import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.enums.AccessibilityNeed;
import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.repository.ReviewReportRepository;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
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

/** Excluir a própria conta (#106): pede a senha, e as avaliações ficam publicadas e anônimas. */
class AccountDeletionIntegrationTest extends AbstractIntegrationTest {

    private static final String SENHA = "senhaForte123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewReportRepository reviewReportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // ---------- helpers ----------

    private User criarUsuario(Role role) {
        LocalDateTime criadoEm = LocalDateTime.now().minusDays(40);
        return userRepository.save(User.builder()
                .name("Pessoa")
                .phone(telefoneUnico())
                .password(passwordEncoder.encode(SENHA))
                .role(role)
                .accessibilityNeeds(Set.of(AccessibilityNeed.MOBILIDADE_REDUZIDA))
                .createdAt(criadoEm)
                .updatedAt(criadoEm)
                .build());
    }

    private MockHttpServletRequestBuilder como(User user, MockHttpServletRequestBuilder requisicao) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.issueAccessToken(user));
    }

    private String json(Map<String, ?> corpo) {
        return objectMapper.writeValueAsString(corpo);
    }

    private ResultActions excluirConta(User user, Map<String, ?> corpo) throws Exception {
        return mockMvc.perform(como(user, delete("/api/users/me"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(corpo)));
    }

    private String avaliar(User autor, String placeId, int nota) throws Exception {
        String corpo = mockMvc.perform(como(autor, post("/api/reviews"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "placeId", placeId,
                                "rating", nota,
                                "comment", "Rampa boa na entrada.",
                                "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asString();
    }

    private String placeUnico() {
        return "ChIJ-" + UUID.randomUUID();
    }

    /** Lê a review com as necessidades carregadas, dentro de uma transação. */
    private Review review(String id) {
        return transactionTemplate.execute(status -> {
            Review review = reviewRepository.findById(id).orElseThrow();
            review.getReviewerNeeds().size();
            return review;
        });
    }

    // ---------- senha ----------

    @Test
    void semSenhaDevolve400ComOCampo() throws Exception {
        User user = criarUsuario(Role.USER);

        excluirConta(user, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.currentPassword").exists());

        assertThat(userRepository.findById(user.getId())).isPresent();
    }

    @Test
    void senhaErradaDevolve400EMantemAConta() throws Exception {
        User user = criarUsuario(Role.USER);

        excluirConta(user, Map.of("currentPassword", "senhaErrada1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Senha atual incorreta"));

        assertThat(userRepository.findById(user.getId())).isPresent();
    }

    @Test
    void depoisDeExcluirLoginERefreshFalham() throws Exception {
        String telefone = telefoneUnico();
        String corpo = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Maria", "phone", telefone, "password", SENHA))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(corpo).get("refreshToken").asString();
        User user = userRepository.findByPhone(telefone).orElseThrow();

        excluirConta(user, Map.of("currentPassword", SENHA)).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", telefone, "password", SENHA))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    // ---------- avaliações ----------

    @Test
    void avaliacoesContinuamPublicadasEAnonimasComANotaIgual() throws Exception {
        User autor = criarUsuario(Role.USER);
        String placeId = placeUnico();
        String reviewId = avaliar(autor, placeId, 5);
        avaliar(criarUsuario(Role.USER), placeId, 3);
        assertThat(review(reviewId).getReviewerNeeds()).isNotEmpty();
        double notaAntes = objectMapper.readTree(mockMvc.perform(get("/api/places/" + placeId))
                .andReturn().getResponse().getContentAsString()).get("averageScore").asDouble();

        excluirConta(autor, Map.of("currentPassword", SENHA)).andExpect(status().isOk());

        Review anonima = review(reviewId);
        assertThat(anonima.getStatus()).isEqualTo(ReviewStatus.PUBLICADA);
        assertThat(anonima.getUserId()).isNotEqualTo(autor.getId());
        assertThat(userRepository.findById(anonima.getUserId())).isEmpty();
        assertThat(anonima.getReviewerNeeds()).isEmpty();
        assertThat(anonima.getComment()).isEqualTo("Rampa boa na entrada.");
        mockMvc.perform(get("/api/places/" + placeId))
                .andExpect(jsonPath("$.averageScore").value(notaAntes))
                .andExpect(jsonPath("$.reviewCount").value(2));
        mockMvc.perform(get("/api/reviews/" + reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mine").value(false));
    }

    @Test
    void duasContasExcluidasComAvaliacaoNoMesmoLocalNaoColidem() throws Exception {
        String placeId = placeUnico();
        User primeira = criarUsuario(Role.USER), segunda = criarUsuario(Role.USER);
        String r1 = avaliar(primeira, placeId, 4);
        String r2 = avaliar(segunda, placeId, 2);

        excluirConta(primeira, Map.of("currentPassword", SENHA)).andExpect(status().isOk());
        excluirConta(segunda, Map.of("currentPassword", SENHA)).andExpect(status().isOk());

        assertThat(review(r1).getUserId()).isNotEqualTo(review(r2).getUserId());
        assertThat(review(r1).getStatus()).isEqualTo(ReviewStatus.PUBLICADA);
        assertThat(review(r2).getStatus()).isEqualTo(ReviewStatus.PUBLICADA);
    }

    @Test
    void denunciasFeitasPelaPessoaSomem() throws Exception {
        String reviewDeOutro = avaliar(criarUsuario(Role.USER), placeUnico(), 4);
        User denunciante = criarUsuario(Role.USER);
        mockMvc.perform(como(denunciante, post("/api/reviews/" + reviewDeOutro + "/report"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "SPAM"))))
                .andExpect(status().isCreated());

        excluirConta(denunciante, Map.of("currentPassword", SENHA)).andExpect(status().isOk());

        assertThat(reviewReportRepository.countByReviewId(reviewDeOutro)).isZero();
    }

    @Test
    void exclusaoPeloAdminTambemAnonimiza() throws Exception {
        User autor = criarUsuario(Role.USER);
        String reviewId = avaliar(autor, placeUnico(), 4);

        mockMvc.perform(como(criarUsuario(Role.ADMIN), delete("/api/users/" + autor.getId())))
                .andExpect(status().isOk());

        Review anonima = review(reviewId);
        assertThat(anonima.getUserId()).isNotEqualTo(autor.getId());
        assertThat(anonima.getReviewerNeeds()).isEmpty();
        assertThat(anonima.getStatus()).isEqualTo(ReviewStatus.PUBLICADA);
    }
}
