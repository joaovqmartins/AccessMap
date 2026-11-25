package br.com.accessmap.backend.review;

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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O nome do local que o app manda ao avaliar volta nas leituras, para listas como "Minhas avaliações". */
class ReviewPlaceNameIntegrationTest extends AbstractIntegrationTest {

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

    private JsonNode avaliar(User autor, String placeId, String placeName) throws Exception {
        Map<String, Object> corpo = new HashMap<>(Map.of(
                "placeId", placeId,
                "rating", 4,
                "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")));
        if (placeName != null) {
            corpo.put("placeName", placeName);
        }
        MvcResult result = mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, bearer(autor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpo)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String placeUnico() {
        return "ChIJ-" + UUID.randomUUID();
    }

    @Test
    void nomeEnviadoAoAvaliarVoltaNaCriacaoNoDetalheENaListaDoUsuario() throws Exception {
        User autor = criarUsuario();
        String placeId = placeUnico();

        JsonNode criada = avaliar(autor, placeId, "Ibirapuera Shopping");
        assertThat(criada.get("placeName").asString()).isEqualTo("Ibirapuera Shopping");

        mockMvc.perform(get("/api/reviews/" + criada.get("id").asString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeName").value("Ibirapuera Shopping"));

        mockMvc.perform(get("/api/reviews?userId=" + autor.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(autor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].placeId").value(placeId))
                .andExpect(jsonPath("$.content[0].placeName").value("Ibirapuera Shopping"));
    }

    @Test
    void localSemNomeDevolvePlaceNameNulo() throws Exception {
        JsonNode criada = avaliar(criarUsuario(), placeUnico(), null);

        assertThat(criada.get("placeName").isNull()).isTrue();
    }

    @Test
    void avaliacaoSemNomeNaoApagaONomeJaSalvo() throws Exception {
        String placeId = placeUnico();
        avaliar(criarUsuario(), placeId, "Padaria Central");

        JsonNode segunda = avaliar(criarUsuario(), placeId, null);

        assertThat(segunda.get("placeName").asString()).isEqualTo("Padaria Central");
    }

    @Test
    void nomeAcimaDe255CaracteresERecusado() throws Exception {
        Map<String, Object> corpo = Map.of(
                "placeId", placeUnico(),
                "placeName", "x".repeat(256),
                "rating", 4,
                "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO"));

        mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, bearer(criarUsuario()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corpo)))
                .andExpect(status().isBadRequest());
    }
}
