package br.com.accessmap.backend;

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
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Texto maior que a coluna ou que o BCrypt aceita responde 400 com o campo, nunca 500 (#110, #111). */
class FieldLimitsIntegrationTest extends AbstractIntegrationTest {

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

    private String json(Map<String, ?> body) {
        return objectMapper.writeValueAsString(body);
    }

    private User criarUsuario() {
        return userRepository.save(User.builder()
                .name("Pessoa")
                .phone(telefoneUnico())
                .password(passwordEncoder.encode(SENHA))
                .role(Role.USER)
                .accessibilityNeeds(Set.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + tokenService.issueAccessToken(user);
    }

    /**
     * E-mail válido com exatamente {@code tamanho} caracteres. O {@code @Email} limita a parte
     * local a 64 caracteres e cada trecho do domínio a 63, então o resto vai em vários trechos.
     * A parte local é única: o banco é compartilhado entre as classes de teste.
     */
    private String emailCom(int tamanho) {
        String local = UUID.randomUUID().toString().replace("-", "");
        String inicio = local + "a".repeat(64 - local.length()) + "@" + "b".repeat(63) + "." + "c".repeat(63) + ".";
        return inicio + "d".repeat(tamanho - inicio.length() - ".com".length()) + ".com";
    }

    private ResultActions cadastrar(Map<String, Object> campos) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("name", "Maria", "phone", telefoneUnico(), "password", SENHA));
        body.putAll(campos);
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    private ResultActions atualizarPerfil(User user, Map<String, Object> body) throws Exception {
        return mockMvc.perform(patch("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    private ResultActions criarReview(User autor, String comentario) throws Exception {
        return mockMvc.perform(post("/api/reviews")
                .header(HttpHeaders.AUTHORIZATION, bearer(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "placeId", "ChIJ-" + UUID.randomUUID(),
                        "rating", 4,
                        "comment", comentario,
                        "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")))));
    }

    private static void campoRecusado(ResultActions result, String campo) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos." + campo).exists());
    }

    // ---------- cadastro ----------

    @Test
    void cadastroAceitaNomeEEmailNoLimite() throws Exception {
        cadastrar(Map.of("name", "n".repeat(255), "email", emailCom(255)))
                .andExpect(status().isCreated());
    }

    @Test
    void cadastroRecusaNomeAcimaDoLimite() throws Exception {
        campoRecusado(cadastrar(Map.of("name", "n".repeat(256))), "name");
    }

    @Test
    void cadastroRecusaEmailAcimaDoLimite() throws Exception {
        campoRecusado(cadastrar(Map.of("email", emailCom(256))), "email");
    }

    @Test
    void cadastroAceitaSenhaDe72Bytes() throws Exception {
        cadastrar(Map.of("password", "s".repeat(72))).andExpect(status().isCreated());
    }

    @Test
    void cadastroAceitaSenhaAcentuadaDe72Bytes() throws Exception {
        // "é" ocupa 2 bytes em UTF-8: 36 caracteres = 72 bytes
        cadastrar(Map.of("password", "é".repeat(36))).andExpect(status().isCreated());
    }

    @Test
    void cadastroRecusaSenhaAcimaDe72Bytes() throws Exception {
        campoRecusado(cadastrar(Map.of("password", "s".repeat(73))), "password");
    }

    @Test
    void cadastroRecusaSenhaAcentuadaAcimaDe72Bytes() throws Exception {
        // 37 caracteres, mas 74 bytes
        campoRecusado(cadastrar(Map.of("password", "é".repeat(37))), "password");
    }

    @Test
    void loginRecusaSenhaAcimaDe72Bytes() throws Exception {
        campoRecusado(mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("phone", telefoneUnico(), "password", "s".repeat(73))))), "password");
    }

    // ---------- perfil ----------

    @Test
    void perfilAceitaNomeEEmailNoLimite() throws Exception {
        atualizarPerfil(criarUsuario(), Map.of("name", "n".repeat(255), "email", emailCom(255)))
                .andExpect(status().isOk());
    }

    @Test
    void perfilRecusaNomeAcimaDoLimite() throws Exception {
        campoRecusado(atualizarPerfil(criarUsuario(), Map.of("name", "n".repeat(256))), "name");
    }

    @Test
    void perfilRecusaEmailAcimaDoLimite() throws Exception {
        campoRecusado(atualizarPerfil(criarUsuario(), Map.of("email", emailCom(256))), "email");
    }

    @Test
    void trocaDeSenhaAceitaSenhaAcentuadaDe72Bytes() throws Exception {
        atualizarPerfil(criarUsuario(), Map.of("password", "é".repeat(36), "currentPassword", SENHA))
                .andExpect(status().isOk());
    }

    @Test
    void trocaDeSenhaRecusaSenhaAcimaDe72Bytes() throws Exception {
        campoRecusado(atualizarPerfil(criarUsuario(),
                Map.of("password", "é".repeat(37), "currentPassword", SENHA)), "password");
    }

    @Test
    void trocaDeSenhaRecusaSenhaAtualAcimaDe72Bytes() throws Exception {
        campoRecusado(atualizarPerfil(criarUsuario(),
                Map.of("password", "novaSenha123", "currentPassword", "s".repeat(73))), "currentPassword");
    }

    // ---------- avaliação ----------

    @Test
    void avaliacaoAceitaComentarioNoLimite() throws Exception {
        criarReview(criarUsuario(), "c".repeat(255)).andExpect(status().isCreated());
    }

    @Test
    void avaliacaoRecusaComentarioAcimaDoLimite() throws Exception {
        campoRecusado(criarReview(criarUsuario(), "c".repeat(256)), "comment");
    }

    @Test
    void edicaoDeAvaliacaoRecusaComentarioAcimaDoLimite() throws Exception {
        User autor = criarUsuario();
        MvcResult criada = criarReview(autor, "Rampa boa.").andExpect(status().isCreated()).andReturn();
        String id = objectMapper.readTree(criada.getResponse().getContentAsString()).get("id").asString();

        mockMvc.perform(patch("/api/reviews/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(autor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("comment", "c".repeat(255)))))
                .andExpect(status().isOk());

        campoRecusado(mockMvc.perform(patch("/api/reviews/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("comment", "c".repeat(256))))), "comment");
    }
}
