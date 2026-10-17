package br.com.accessmap.backend.place;

import br.com.accessmap.backend.AbstractIntegrationTest;
import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O banco é compartilhado com as outras classes de integração, então cada teste cria locais com
 * {@code placeId} únicos e só afirma sobre eles (presença, ausência e ordem relativa).
 */
class PlaceSearchIntegrationTest extends AbstractIntegrationTest {

    private static final String RAMPAS = "RAMPAS_E_ENTRADAS";
    private static final String ELEVADORES = "ELEVADORES";

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

    private String placeUnico() {
        return "ChIJ-" + UUID.randomUUID();
    }

    private void avaliar(String placeId, int nota, Map<String, String> tags) throws Exception {
        String token = "Bearer " + tokenService.issueAccessToken(criarUsuario());
        mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placeId", placeId,
                                "rating", nota,
                                "comment", "Teste de busca.",
                                "tags", tags))))
                .andExpect(status().isCreated());
    }

    /** placeIds devolvidos pela busca, na ordem em que vieram. */
    private List<String> buscar(String query) throws Exception {
        String corpo = mockMvc.perform(get("/api/places?size=1000" + query))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ids = new ArrayList<>();
        objectMapper.readTree(corpo).get("content").forEach((JsonNode lugar) -> ids.add(lugar.get("placeId").asString()));
        return ids;
    }

    // ---------- filtro por tag ----------

    @Test
    void filtraPorUmaTag() throws Exception {
        String a = placeUnico(), b = placeUnico(), c = placeUnico();
        avaliar(a, 5, Map.of(RAMPAS, "ADEQUADO", ELEVADORES, "ADEQUADO"));
        avaliar(b, 3, Map.of(RAMPAS, "ADEQUADO", ELEVADORES, "INEXISTENTE"));
        avaliar(c, 4, Map.of(RAMPAS, "INADEQUADO"));

        List<String> ids = buscar("&tags=" + RAMPAS);

        assertThat(ids).contains(a, b).doesNotContain(c);
    }

    @Test
    void exigeTodasAsTagsPedidas() throws Exception {
        String a = placeUnico(), b = placeUnico(), c = placeUnico();
        avaliar(a, 5, Map.of(RAMPAS, "ADEQUADO", ELEVADORES, "ADEQUADO"));
        avaliar(b, 3, Map.of(RAMPAS, "ADEQUADO", ELEVADORES, "INEXISTENTE"));
        avaliar(c, 4, Map.of(RAMPAS, "INADEQUADO"));

        List<String> ids = buscar("&tags=" + RAMPAS + "," + ELEVADORES);

        assertThat(ids).contains(a).doesNotContain(b, c);
    }

    @Test
    void localSemAvaliacaoNaTagNaoPassa() throws Exception {
        String soRampa = placeUnico();
        avaliar(soRampa, 5, Map.of(RAMPAS, "ADEQUADO"));

        assertThat(buscar("&tags=" + ELEVADORES)).doesNotContain(soRampa);
    }

    // ---------- regra de maioria estrita ----------

    @Test
    void votosEmpatadosNaoPassamEMaioriaPassa() throws Exception {
        String empate = placeUnico(), maioria = placeUnico();

        avaliar(empate, 4, Map.of(RAMPAS, "ADEQUADO"));
        avaliar(empate, 2, Map.of(RAMPAS, "INADEQUADO"));

        avaliar(maioria, 4, Map.of(RAMPAS, "ADEQUADO"));
        avaliar(maioria, 4, Map.of(RAMPAS, "ADEQUADO"));
        avaliar(maioria, 2, Map.of(RAMPAS, "INADEQUADO"));

        List<String> ids = buscar("&tags=" + RAMPAS);

        assertThat(ids).contains(maioria).doesNotContain(empate);
    }

    // ---------- ordenação e paginação ----------

    @Test
    void ordenaPelaNotaMediaDoMaiorParaOMenor() throws Exception {
        String nota5 = placeUnico(), nota3 = placeUnico();
        avaliar(nota3, 3, Map.of(RAMPAS, "ADEQUADO"));
        avaliar(nota5, 5, Map.of(RAMPAS, "ADEQUADO"));

        List<String> ids = buscar("&tags=" + RAMPAS);

        assertThat(ids.indexOf(nota5)).isLessThan(ids.indexOf(nota3));
    }

    @Test
    void paginaOsResultados() throws Exception {
        avaliar(placeUnico(), 5, Map.of(RAMPAS, "ADEQUADO"));
        avaliar(placeUnico(), 4, Map.of(RAMPAS, "ADEQUADO"));

        mockMvc.perform(get("/api/places?tags=" + RAMPAS + "&size=1&page=0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$.page.totalPages").isNumber());
    }

    // ---------- sem filtro, acesso e erros ----------

    @Test
    void semTagsListaTodosOsLocais() throws Exception {
        String a = placeUnico(), c = placeUnico();
        avaliar(a, 5, Map.of(RAMPAS, "ADEQUADO"));
        avaliar(c, 4, Map.of(RAMPAS, "INADEQUADO"));

        assertThat(buscar("")).contains(a, c);
    }

    @Test
    void buscaNaoExigeLogin() throws Exception {
        mockMvc.perform(get("/api/places?tags=" + RAMPAS))
                .andExpect(status().isOk());
    }

    @Test
    void tagInexistenteDevolve400ComOsValoresAceitos() throws Exception {
        mockMvc.perform(get("/api/places?tags=BANANA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("BANANA")))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString(RAMPAS)));
    }
}
