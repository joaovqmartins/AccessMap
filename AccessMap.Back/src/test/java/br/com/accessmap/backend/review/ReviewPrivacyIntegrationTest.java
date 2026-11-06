package br.com.accessmap.backend.review;

import br.com.accessmap.backend.AbstractIntegrationTest;
import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.enums.AccessibilityNeed;
import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A API pública não pode ligar uma avaliação a quem a escreveu nem à deficiência declarada (LGPD, #114). */
class ReviewPrivacyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    // ---------- helpers ----------

    private User criarUsuario(Role role) {
        return userRepository.save(User.builder()
                .name("Pessoa")
                .phone(telefoneUnico())
                .password(passwordEncoder.encode("senhaForte123"))
                .role(role)
                .accessibilityNeeds(Set.of(AccessibilityNeed.MOBILIDADE_REDUZIDA))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private User usuario() {
        return criarUsuario(Role.USER);
    }

    private MockHttpServletRequestBuilder como(User user, MockHttpServletRequestBuilder requisicao) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.issueAccessToken(user));
    }

    private String placeUnico() {
        return "ChIJ-" + UUID.randomUUID();
    }

    private String criarReview(User autor, String placeId) throws Exception {
        String corpo = mockMvc.perform(como(autor, post("/api/reviews"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placeId", placeId,
                                "rating", 4,
                                "comment", "Teste de privacidade.",
                                "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mine").value(true))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("id").asString();
    }

    // ---------- resposta pública ----------

    @Test
    void respostaAnonimaNaoTemQuemAvaliouNemANecessidade() throws Exception {
        User autor = usuario();
        String placeId = placeUnico();
        String reviewId = criarReview(autor, placeId);

        String detalhe = mockMvc.perform(get("/api/reviews/" + reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.reviewerNeeds").doesNotExist())
                .andExpect(jsonPath("$.mine").value(false))
                .andReturn().getResponse().getContentAsString();
        String lista = mockMvc.perform(get("/api/reviews").param("placeId", placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.content[0].reviewerNeeds").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(detalhe).doesNotContain(autor.getId(), "MOBILIDADE_REDUZIDA");
        assertThat(lista).doesNotContain(autor.getId(), "MOBILIDADE_REDUZIDA");
    }

    @Test
    void outraPessoaLogadaTambemNaoVeEMineVemFalso() throws Exception {
        String reviewId = criarReview(usuario(), placeUnico());

        mockMvc.perform(como(usuario(), get("/api/reviews/" + reviewId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mine").value(false))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.reviewerNeeds").doesNotExist());
    }

    @Test
    void oAutorVeMineENecessidadesDaPropriaAvaliacao() throws Exception {
        User autor = usuario();
        String placeId = placeUnico();
        String reviewId = criarReview(autor, placeId);

        mockMvc.perform(como(autor, get("/api/reviews/" + reviewId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mine").value(true))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.reviewerNeeds[0]").value("MOBILIDADE_REDUZIDA"));
        mockMvc.perform(como(autor, get("/api/reviews")).param("placeId", placeId))
                .andExpect(jsonPath("$.content[0].mine").value(true));
    }

    @Test
    void adminEditandoAvaliacaoDeOutroNaoRecebeOAutorNaResposta() throws Exception {
        User autor = usuario();
        String reviewId = criarReview(autor, placeUnico());

        String corpo = mockMvc.perform(como(criarUsuario(Role.ADMIN), patch("/api/reviews/" + reviewId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mine").value(false))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).doesNotContain(autor.getId());
    }

    // ---------- filtro por usuário ----------

    @Test
    void filtrarPorUsuarioSemLoginDevolve401() throws Exception {
        User autor = usuario();
        criarReview(autor, placeUnico());

        mockMvc.perform(get("/api/reviews").param("userId", autor.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void filtrarAsAvaliacoesDeOutroUsuarioDevolve403() throws Exception {
        User autor = usuario();
        criarReview(autor, placeUnico());

        mockMvc.perform(como(usuario(), get("/api/reviews")).param("userId", autor.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void filtroComPlaceIdTambemEProtegido() throws Exception {
        User autor = usuario();
        String placeId = placeUnico();
        criarReview(autor, placeId);

        mockMvc.perform(como(usuario(), get("/api/reviews"))
                        .param("userId", autor.getId())
                        .param("placeId", placeId))
                .andExpect(status().isForbidden());
    }

    @Test
    void oProprioUsuarioListaAsSuasAvaliacoes() throws Exception {
        User autor = usuario();
        criarReview(autor, placeUnico());

        mockMvc.perform(como(autor, get("/api/reviews")).param("userId", autor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].mine").value(true));
    }

    @Test
    void adminPodeFiltrarPorQualquerUsuario() throws Exception {
        User autor = usuario();
        criarReview(autor, placeUnico());

        mockMvc.perform(como(criarUsuario(Role.ADMIN), get("/api/reviews")).param("userId", autor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void listarPorLocalContinuaPublico() throws Exception {
        String placeId = placeUnico();
        criarReview(usuario(), placeId);

        mockMvc.perform(get("/api/reviews").param("placeId", placeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }
}
