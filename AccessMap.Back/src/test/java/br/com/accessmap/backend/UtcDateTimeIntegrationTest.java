package br.com.accessmap.backend;

import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A suíte roda com a máquina no fuso de Brasília ({@code -Duser.timezone} no pom): estes testes provam que a
 * API grava e envia as datas em UTC mesmo assim (#115).
 */
class UtcDateTimeIntegrationTest extends AbstractIntegrationTest {

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

    private JsonNode criarReview() throws Exception {
        User autor = userRepository.save(User.builder()
                .name("Pessoa")
                .phone(telefoneUnico())
                .password(passwordEncoder.encode("senhaForte123"))
                .role(Role.USER)
                .accessibilityNeeds(Set.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        String corpo = mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.issueAccessToken(autor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "placeId", "ChIJ-" + UUID.randomUUID(),
                                "rating", 4,
                                "comment", "Teste de fuso.",
                                "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo);
    }

    @Test
    void aApiFixaOFusoEmUtcMesmoComAMaquinaEmBrasilia() {
        assertThat(System.getProperty("user.timezone")).isEqualTo("America/Sao_Paulo");
        assertThat(TimeZone.getDefault().getRawOffset()).isZero();
    }

    @Test
    void aDataNoJsonTemOFusoEMarcaOMomentoCerto() throws Exception {
        Instant antes = Instant.now();
        JsonNode review = criarReview();

        String criadaEm = review.get("createdAt").asString();

        assertThat(criadaEm).endsWith("Z");
        assertThat(Duration.between(antes, Instant.parse(criadaEm)).abs()).isLessThan(Duration.ofSeconds(30));
    }

    @Test
    void oBancoGuardaEmUtc() throws Exception {
        String id = criarReview().get("id").asString();

        LocalDateTime gravada = reviewRepository.findById(id).orElseThrow().getCreatedAt();

        assertThat(Duration.between(gravada, LocalDateTime.now(ZoneOffset.UTC)).abs()).isLessThan(Duration.ofSeconds(30));
    }

    @Test
    void aLeituraPublicaTambemTemOFuso() throws Exception {
        String id = criarReview().get("id").asString();

        String corpo = mockMvc.perform(get("/api/reviews/" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(corpo).get("createdAt").asString()).endsWith("Z");
        assertThat(objectMapper.readTree(corpo).get("updatedAt").asString()).endsWith("Z");
    }
}
