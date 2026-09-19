package br.com.accessmap.backend.identity;

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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAccessIntegrationTest extends AbstractIntegrationTest {

    private static final String SENHA = "senhaForte123";

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

    private String emailUnico() {
        return "user-" + UUID.randomUUID() + "@teste.com";
    }

    private String json(Map<String, ?> body) {
        return objectMapper.writeValueAsString(body);
    }

    /** Registra pela API e devolve o corpo com tokens. */
    private JsonNode registrar(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Maria",
                                "email", email,
                                "password", SENHA,
                                "phone", "11999999999",
                                "age", 30,
                                "accessibilityNeeds", new String[]{"MOBILIDADE_REDUZIDA"}))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** Cria um ADMIN direto no banco (não há endpoint para isso) e devolve um access token dele. */
    private String tokenDeAdmin() {
        User admin = userRepository.save(User.builder()
                .name("Admin")
                .email(emailUnico())
                .password(passwordEncoder.encode(SENHA))
                .role(Role.ADMIN)
                .accessibilityNeeds(Set.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        return tokenService.issueAccessToken(admin);
    }

    private String bearer(JsonNode tokens) {
        return "Bearer " + tokens.get("accessToken").asString();
    }

    // ---------- /me ----------

    @Test
    void meDevolveOProprioUsuarioSemSenha() throws Exception {
        String email = emailUnico();
        JsonNode tokens = registrar(email);

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void patchMeAtualizaApenasOsCamposEnviados() throws Exception {
        String email = emailUnico();
        JsonNode tokens = registrar(email);

        mockMvc.perform(patch("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Maria Silva"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void trocaDeSenhaExigeSenhaAtualCorreta() throws Exception {
        JsonNode tokens = registrar(emailUnico());

        mockMvc.perform(patch("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("password", "outraSenha123", "currentPassword", "errada"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Senha atual incorreta"));

        mockMvc.perform(patch("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("password", "outraSenha123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void trocaDeSenhaEncerraAsSessoesExistentes() throws Exception {
        String email = emailUnico();
        JsonNode tokens = registrar(email);
        String refreshAntigo = tokens.get("refreshToken").asString();

        mockMvc.perform(patch("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("password", "outraSenha123", "currentPassword", SENHA))))
                .andExpect(status().isOk());

        // o refresh emitido antes da troca não serve mais
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshAntigo))))
                .andExpect(status().isUnauthorized());

        // e a senha nova é a que vale no login
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "outraSenha123"))))
                .andExpect(status().isOk());
    }

    @Test
    void deleteMeRemoveAContaEOTokenDeixaDeResolverUmUsuario() throws Exception {
        JsonNode tokens = registrar(emailUnico());

        mockMvc.perform(delete("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(status().isOk());

        // o JWT ainda é criptograficamente válido, mas o usuário não existe mais
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(status().isNotFound());
    }

    // ---------- administração ----------

    @Test
    void usuarioComumNaoAcessaRotasAdministrativas() throws Exception {
        JsonNode tokens = registrar(emailUnico());
        String outroId = registrar(emailUnico()).get("user").get("id").asString();

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value("Acesso negado"));

        mockMvc.perform(get("/api/users/" + outroId).header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/users/" + outroId).header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListaBuscaERemoveUsuarios() throws Exception {
        String adminToken = "Bearer " + tokenDeAdmin();
        String alvoId = registrar(emailUnico()).get("user").get("id").asString();

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].password").doesNotExist());

        mockMvc.perform(get("/api/users/" + alvoId).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alvoId));

        mockMvc.perform(delete("/api/users/" + alvoId).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/" + alvoId).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void cadastroPublicoDeUsuarioNaoExisteMais() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "x"))))
                .andExpect(status().isUnauthorized());
    }
}
