package br.com.accessmap.backend.auth;

import br.com.accessmap.backend.AbstractIntegrationTest;
import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String SENHA = "senhaForte123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    // ---------- helpers ----------

    private String emailUnico() {
        return "user-" + UUID.randomUUID() + "@teste.com";
    }

    private String json(Map<String, ?> body) {
        return objectMapper.writeValueAsString(body);
    }

    private Map<String, Object> cadastroValido(String email) {
        return Map.of(
                "name", "Maria",
                "email", email,
                "password", SENHA,
                "phone", "11999999999",
                "age", 30,
                "accessibilityNeeds", new String[]{"MOBILIDADE_REDUZIDA"}
        );
    }

    private JsonNode registrar(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cadastroValido(email))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode refresh(String refreshToken, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    // ---------- cadastro ----------

    @Test
    void registroDevolveTokensESenhaNuncaAparece() throws Exception {
        JsonNode body = registrar(emailUnico());

        assertThat(body.get("accessToken").asString()).isNotBlank();
        assertThat(body.get("refreshToken").asString()).isNotBlank();
        assertThat(body.get("tokenType").asString()).isEqualTo("Bearer");
        assertThat(body.get("expiresIn").asLong()).isEqualTo(15 * 60);
        assertThat(body.get("user").get("role").asString()).isEqualTo("USER");
        assertThat(body.get("user").has("password")).isFalse();
    }

    @Test
    void registroRejeitaEmailDuplicado() throws Exception {
        String email = emailUnico();
        registrar(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cadastroValido(email))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("E-mail já está em uso"));
    }

    @Test
    void registroValidaCamposObrigatorios() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nao-eh-email", "password", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.name").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.password").exists())
                .andExpect(jsonPath("$.campos.accessibilityNeeds").exists());
    }

    // ---------- login ----------

    @Test
    void loginComCredenciaisCorretasEmiteTokens() throws Exception {
        String email = emailUnico();
        registrar(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", SENHA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void loginNaoRevelaSeFoiEmailOuSenhaQueErrou() throws Exception {
        String email = emailUnico();
        registrar(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "senhaErrada"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Credenciais inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "ninguem@teste.com", "password", SENHA))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Credenciais inválidas"));
    }

    // ---------- proteção de rotas ----------

    @Test
    void rotaProtegidaSemTokenDevolve401Json() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value("Autenticação necessária"));
    }

    @Test
    void rotaProtegidaComTokenValidoPassa() throws Exception {
        JsonNode body = registrar(emailUnico());

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(body.get("accessToken").asString())))
                .andExpect(status().isOk());
    }

    @Test
    void rotaPublicaNaoExigeToken() throws Exception {
        // 404 (e não 401): a regra de acesso deixou passar e o controller respondeu
        mockMvc.perform(get("/api/places/lugar-que-nao-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void tokenExpiradoDevolve401() throws Exception {
        String email = emailUnico();
        registrar(email);
        User user = userRepository.findByEmail(email).orElseThrow();

        // emitido há 10 min com validade de 5: expirou há 5 min, além da tolerância de 60s do validador
        String expirado = tokenService.issueAccessToken(user, Instant.now().minus(Duration.ofMinutes(10)), Duration.ofMinutes(5));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(expirado)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Token inválido ou expirado"));
    }

    @Test
    void tokenAdulteradoDevolve401() throws Exception {
        JsonNode body = registrar(emailUnico());
        String token = body.get("accessToken").asString();
        String adulterado = token.substring(0, token.length() - 4) + "abcd";

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(adulterado)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Token inválido ou expirado"));
    }

    // ---------- refresh ----------

    @Test
    void refreshRotacionaEInvalidaOAnterior() throws Exception {
        JsonNode inicial = registrar(emailUnico());
        String refresh1 = inicial.get("refreshToken").asString();

        JsonNode segundo = refresh(refresh1, 200);
        String refresh2 = segundo.get("refreshToken").asString();

        assertThat(refresh2).isNotEqualTo(refresh1);
        assertThat(segundo.get("accessToken").asString()).isNotBlank();

        // o token consumido não serve mais
        refresh(refresh1, 401);
    }

    @Test
    void reusoDeRefreshRevogadoDerrubaTodasAsSessoes() throws Exception {
        JsonNode inicial = registrar(emailUnico());
        String refresh1 = inicial.get("refreshToken").asString();

        String refresh2 = refresh(refresh1, 200).get("refreshToken").asString();

        // reapresentar refresh1 (já revogado) sinaliza roubo...
        JsonNode erro = refresh(refresh1, 401);
        assertThat(erro.get("mensagem").asString()).isEqualTo("Sessão inválida. Faça login novamente.");

        // ...e o refresh2, que era válido, também cai
        refresh(refresh2, 401);
    }

    @Test
    void refreshInexistenteDevolve401() throws Exception {
        refresh("token-que-nunca-existiu", 401);
    }

    // ---------- logout ----------

    @Test
    void logoutRevogaORefreshToken() throws Exception {
        JsonNode body = registrar(emailUnico());
        String refreshToken = body.get("refreshToken").asString();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk());

        refresh(refreshToken, 401);
    }

    @Test
    void logoutEIdempotente() throws Exception {
        JsonNode body = registrar(emailUnico());
        String refreshToken = body.get("refreshToken").asString();

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", refreshToken))))
                    .andExpect(status().isOk());
        }
    }
}
