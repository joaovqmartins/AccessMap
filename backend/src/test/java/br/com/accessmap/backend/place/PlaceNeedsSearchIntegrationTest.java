package br.com.accessmap.backend.place;

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
 * Busca pelas necessidades do perfil ({@code useMyNeeds}). Mesmo esquema do {@link PlaceSearchIntegrationTest}:
 * banco compartilhado, então cada teste usa {@code placeId}s únicos e só afirma sobre eles.
 */
class PlaceNeedsSearchIntegrationTest extends AbstractIntegrationTest {

    private static final String RAMPAS = "RAMPAS_E_ENTRADAS";
    private static final String BANHEIROS = "BANHEIROS_ADAPTADOS";
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

    private String tokenDe(AccessibilityNeed... needs) {
        User user = userRepository.save(User.builder()
                .name("Pessoa")
                .phone(telefoneUnico())
                .password(passwordEncoder.encode("senhaForte123"))
                .role(Role.USER)
                .accessibilityNeeds(Set.of(needs))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        return "Bearer " + tokenService.issueAccessToken(user);
    }

    private String placeUnico() {
        return "ChIJ-" + UUID.randomUUID();
    }

    private void avaliar(String placeId, Map<String, String> tags) throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(AccessibilityNeed.OUTROS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placeId", placeId, "rating", 4, "comment", "Teste de necessidades.", "tags", tags))))
                .andExpect(status().isCreated());
    }

    private List<String> ids(String corpoJson, boolean paginado) {
        JsonNode raiz = objectMapper.readTree(corpoJson);
        List<String> ids = new ArrayList<>();
        (paginado ? raiz.get("content") : raiz).forEach(lugar -> ids.add(lugar.get("placeId").asString()));
        return ids;
    }

    private List<String> buscar(String token, String query) throws Exception {
        var requisicao = get("/api/places?size=1000" + query);
        if (token != null) {
            requisicao.header(HttpHeaders.AUTHORIZATION, token);
        }
        return ids(mockMvc.perform(requisicao).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), true);
    }

    // ---------- busca ----------

    @Test
    void useMyNeedsFiltraPelasTagsEssenciaisDoPerfil() throws Exception {
        String completo = placeUnico(), soRampa = placeUnico();
        avaliar(completo, Map.of(RAMPAS, "ADEQUADO", BANHEIROS, "ADEQUADO"));
        avaliar(soRampa, Map.of(RAMPAS, "ADEQUADO"));

        List<String> ids = buscar(tokenDe(AccessibilityNeed.MOBILIDADE_REDUZIDA), "&useMyNeeds=true");

        assertThat(ids).contains(completo).doesNotContain(soRampa);
    }

    @Test
    void useMyNeedsSomaComAsTagsEscolhidasNaHora() throws Exception {
        String comElevador = placeUnico(), semElevador = placeUnico();
        avaliar(comElevador, Map.of(RAMPAS, "ADEQUADO", BANHEIROS, "ADEQUADO", ELEVADORES, "ADEQUADO"));
        avaliar(semElevador, Map.of(RAMPAS, "ADEQUADO", BANHEIROS, "ADEQUADO"));

        List<String> ids = buscar(tokenDe(AccessibilityNeed.MOBILIDADE_REDUZIDA),
                "&useMyNeeds=true&tags=" + ELEVADORES);

        assertThat(ids).contains(comElevador).doesNotContain(semElevador);
    }

    @Test
    void semUseMyNeedsOPerfilNaoInterfereMesmoLogado() throws Exception {
        String soRampa = placeUnico();
        avaliar(soRampa, Map.of(RAMPAS, "ADEQUADO"));

        assertThat(buscar(tokenDe(AccessibilityNeed.MOBILIDADE_REDUZIDA), "")).contains(soRampa);
    }

    @Test
    void perfilSoComOutrosNaoFiltraNada() throws Exception {
        String soRampa = placeUnico(), reprovado = placeUnico();
        avaliar(soRampa, Map.of(RAMPAS, "ADEQUADO"));
        avaliar(reprovado, Map.of(RAMPAS, "INADEQUADO"));

        assertThat(buscar(tokenDe(AccessibilityNeed.OUTROS), "&useMyNeeds=true")).contains(soRampa, reprovado);
    }

    @Test
    void useMyNeedsSemLoginDevolve401() throws Exception {
        mockMvc.perform(get("/api/places?useMyNeeds=true"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ---------- busca em lote ----------

    @Test
    void buscaEmLoteComUseMyNeedsDevolveSoOsQuePassamNaOrdemPedida() throws Exception {
        String completo = placeUnico(), soRampa = placeUnico(), outroCompleto = placeUnico();
        avaliar(completo, Map.of(RAMPAS, "ADEQUADO", BANHEIROS, "ADEQUADO"));
        avaliar(soRampa, Map.of(RAMPAS, "ADEQUADO"));
        avaliar(outroCompleto, Map.of(RAMPAS, "ADEQUADO", BANHEIROS, "ADEQUADO"));

        String corpo = mockMvc.perform(post("/api/places/batch")
                        .header(HttpHeaders.AUTHORIZATION, tokenDe(AccessibilityNeed.MOBILIDADE_REDUZIDA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placeIds", List.of(outroCompleto, soRampa, completo), "useMyNeeds", true))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(ids(corpo, false)).containsExactly(outroCompleto, completo);
    }

    @Test
    void buscaEmLoteComUseMyNeedsSemLoginDevolve401() throws Exception {
        mockMvc.perform(post("/api/places/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placeIds", List.of(placeUnico()), "useMyNeeds", true))))
                .andExpect(status().isUnauthorized());
    }
}
