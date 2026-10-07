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
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaceBatchIntegrationTest extends AbstractIntegrationTest {

    private static final String RAMPAS = "RAMPAS_E_ENTRADAS";

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

    private String placeUnico() {
        return "ChIJ-" + UUID.randomUUID();
    }

    private void avaliar(String placeId, int nota, Map<String, String> tags) throws Exception {
        User autor = userRepository.save(User.builder()
                .name("Pessoa")
                .phone(telefoneUnico())
                .password(passwordEncoder.encode("senhaForte123"))
                .role(Role.USER)
                .accessibilityNeeds(Set.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.issueAccessToken(autor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placeId", placeId, "rating", nota, "comment", "Teste em lote.", "tags", tags))))
                .andExpect(status().isCreated());
    }

    private ResultActions lote(Object corpo) throws Exception {
        return mockMvc.perform(post("/api/places/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo instanceof String s ? s : objectMapper.writeValueAsString(corpo)));
    }

    // ---------- agregados e ordem ----------

    @Test
    void devolveOsAgregadosNaOrdemPedida() throws Exception {
        String a = placeUnico(), b = placeUnico();
        avaliar(a, 5, Map.of(RAMPAS, "ADEQUADO"));
        avaliar(b, 3, Map.of(RAMPAS, "INADEQUADO"));

        lote(Map.of("placeIds", List.of(b, a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].placeId").value(b))
                .andExpect(jsonPath("$[0].averageScore").value(3.0))
                .andExpect(jsonPath("$[0].reviewCount").value(1))
                .andExpect(jsonPath("$[1].placeId").value(a))
                .andExpect(jsonPath("$[1].averageScore").value(5.0))
                .andExpect(jsonPath("$[1].tagStats." + RAMPAS + ".adequadoCount").value(1));
    }

    @Test
    void localSemAvaliacaoVoltaZeradoEMantemAPosicao() throws Exception {
        String avaliado = placeUnico(), desconhecido = placeUnico();
        avaliar(avaliado, 4, Map.of(RAMPAS, "ADEQUADO"));

        lote(Map.of("placeIds", List.of(desconhecido, avaliado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placeId").value(desconhecido))
                .andExpect(jsonPath("$[0].averageScore").value(0.0))
                .andExpect(jsonPath("$[0].reviewCount").value(0))
                .andExpect(jsonPath("$[0].tagStats").isEmpty())
                .andExpect(jsonPath("$[1].placeId").value(avaliado));
    }

    @Test
    void idsRepetidosViramUmSoResultado() throws Exception {
        String a = placeUnico();
        avaliar(a, 4, Map.of(RAMPAS, "ADEQUADO"));

        lote(Map.of("placeIds", List.of(a, a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void buscaEmLoteNaoExigeLogin() throws Exception {
        lote(Map.of("placeIds", List.of(placeUnico()))).andExpect(status().isOk());
    }

    // ---------- filtro por tags ----------

    @Test
    void comTagsSoVoltamOsLocaisQueTemTodasElas() throws Exception {
        String passa = placeUnico(), reprova = placeUnico(), semAvaliacao = placeUnico();
        avaliar(passa, 5, Map.of(RAMPAS, "ADEQUADO"));
        avaliar(reprova, 4, Map.of(RAMPAS, "INADEQUADO"));

        lote(Map.of("placeIds", List.of(semAvaliacao, reprova, passa), "tags", List.of(RAMPAS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placeId").value(passa));
    }

    // ---------- validação ----------

    @Test
    void listaVaziaDevolve400() throws Exception {
        lote(Map.of("placeIds", List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.placeIds").exists());
    }

    @Test
    void semPlaceIdsDevolve400() throws Exception {
        lote("{}").andExpect(status().isBadRequest());
    }

    @Test
    void maisDe60IdsDevolve400() throws Exception {
        List<String> ids = IntStream.range(0, 61).mapToObj(i -> placeUnico()).toList();

        lote(Map.of("placeIds", ids))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.placeIds").value(containsString("60")));
    }

    @Test
    void exatamente60IdsEhAceito() throws Exception {
        List<String> ids = IntStream.range(0, 60).mapToObj(i -> placeUnico()).toList();

        lote(Map.of("placeIds", ids))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(60));
    }

    @Test
    void placeIdEmBrancoDevolve400() throws Exception {
        lote(Map.of("placeIds", Collections.singletonList(" "))).andExpect(status().isBadRequest());
    }

    @Test
    void tagInexistenteNoCorpoDevolve400ComOsValoresAceitos() throws Exception {
        lote("{\"placeIds\":[\"x\"],\"tags\":[\"BANANA\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("BANANA")))
                .andExpect(jsonPath("$.mensagem").value(containsString(RAMPAS)));
    }

    @Test
    void jsonQuebradoDevolve400() throws Exception {
        lote("{placeIds:").andExpect(status().isBadRequest());
    }
}
