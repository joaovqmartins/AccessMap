package br.com.accessmap.backend.auth;

import br.com.accessmap.backend.AbstractIntegrationTest;
import br.com.accessmap.backend.auth.model.AuthAttempt;
import br.com.accessmap.backend.auth.repository.AuthAttemptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Limites baixos só nesta classe; cada teste usa IP e telefones próprios para não contar as falhas dos outros. */
@TestPropertySource(properties = {
        "accessmap.auth.rate-limit.window=15m",
        "accessmap.auth.rate-limit.login-per-phone=3",
        "accessmap.auth.rate-limit.login-per-ip=6",
        "accessmap.auth.rate-limit.register-per-ip=2"
})
class AuthRateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final String SENHA = "senhaForte123";
    private static final String ERRADA = "senhaErrada1";
    private static final AtomicInteger PROXIMO_IP = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthAttemptRepository authAttemptRepository;

    // ---------- helpers ----------

    private String ipUnico() {
        return "10.0.0." + PROXIMO_IP.getAndIncrement();
    }

    private ResultActions enviar(String rota, String ip, Map<String, ?> corpo) throws Exception {
        return mockMvc.perform(post(rota)
                .with(requisicao -> {
                    requisicao.setRemoteAddr(ip);
                    return requisicao;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo)));
    }

    private ResultActions login(String telefone, String senha, String ip) throws Exception {
        return enviar("/api/auth/login", ip, Map.of("phone", telefone, "password", senha));
    }

    private ResultActions cadastro(String telefone, String ip) throws Exception {
        return enviar("/api/auth/register", ip, Map.of("name", "Maria", "phone", telefone, "password", SENHA));
    }

    private String contaNova() throws Exception {
        String telefone = telefoneUnico();
        cadastro(telefone, ipUnico()).andExpect(status().isCreated());
        return telefone;
    }

    private void errar(String telefone, String ip, int vezes) throws Exception {
        for (int i = 0; i < vezes; i++) {
            login(telefone, ERRADA, ip).andExpect(status().isUnauthorized());
        }
    }

    /** Simula a passagem do tempo sem esperar a janela de verdade. */
    private void passarDaJanela() {
        List<AuthAttempt> tentativas = authAttemptRepository.findAll();
        tentativas.forEach(tentativa -> tentativa.setCreatedAt(tentativa.getCreatedAt().minusMinutes(16)));
        authAttemptRepository.saveAll(tentativas);
    }

    // ---------- login por telefone ----------

    @Test
    void bloqueiaNoLimiteMesmoComASenhaCerta() throws Exception {
        String telefone = contaNova();
        String ip = ipUnico();
        errar(telefone, ip, 3);

        login(telefone, SENHA, ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.mensagem").value(containsString("Tente novamente em")));
    }

    @Test
    void desbloqueiaQuandoAJanelaPassa() throws Exception {
        String telefone = contaNova();
        String ip = ipUnico();
        errar(telefone, ip, 3);
        login(telefone, SENHA, ip).andExpect(status().isTooManyRequests());

        passarDaJanela();

        login(telefone, SENHA, ip).andExpect(status().isOk());
    }

    @Test
    void loginCorretoZeraOContadorDoTelefone() throws Exception {
        String telefone = contaNova();
        errar(telefone, ipUnico(), 2);

        login(telefone, SENHA, ipUnico()).andExpect(status().isOk());

        errar(telefone, ipUnico(), 2);
        login(telefone, SENHA, ipUnico()).andExpect(status().isOk());
    }

    @Test
    void oLimiteDoTelefoneValeVindoDeIpsDiferentes() throws Exception {
        String telefone = contaNova();
        errar(telefone, ipUnico(), 1);
        errar(telefone, ipUnico(), 1);
        errar(telefone, ipUnico(), 1);

        login(telefone, SENHA, ipUnico()).andExpect(status().isTooManyRequests());
    }

    @Test
    void telefoneInexistenteBloqueiaDoMesmoJeito() throws Exception {
        String inexistente = telefoneUnico();
        String ip = ipUnico();
        errar(inexistente, ip, 3);

        login(inexistente, ERRADA, ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.mensagem").value(containsString("Tente novamente em")));
    }

    @Test
    void quemJaEstaLogadoContinuaLogadoDuranteOBloqueio() throws Exception {
        String telefone = telefoneUnico();
        String corpo = cadastro(telefone, ipUnico()).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(corpo).get("refreshToken").asString();
        errar(telefone, ipUnico(), 3);

        enviar("/api/auth/refresh", ipUnico(), Map.of("refreshToken", refreshToken)).andExpect(status().isOk());
    }

    // ---------- login por IP ----------

    @Test
    void oLimitePorIpPegaQuemTestaVariosTelefones() throws Exception {
        String ip = ipUnico();
        for (int i = 0; i < 6; i++) {
            errar(telefoneUnico(), ip, 1);
        }

        login(telefoneUnico(), ERRADA, ip).andExpect(status().isTooManyRequests());
        login(telefoneUnico(), ERRADA, ipUnico()).andExpect(status().isUnauthorized());
    }

    // ---------- cadastro ----------

    @Test
    void cadastroComDadosEmUsoBloqueiaOIp() throws Exception {
        String existente = contaNova();
        String ip = ipUnico();
        cadastro(existente, ip).andExpect(status().isConflict());
        cadastro(existente, ip).andExpect(status().isConflict());

        cadastro(telefoneUnico(), ip).andExpect(status().isTooManyRequests());
        cadastro(telefoneUnico(), ipUnico()).andExpect(status().isCreated());
    }

    @Test
    void cadastroValidoNaoConta() throws Exception {
        String ip = ipUnico();

        for (int i = 0; i < 4; i++) {
            cadastro(telefoneUnico(), ip).andExpect(status().isCreated());
        }
    }

    // ---------- o que fica guardado ----------

    @Test
    void naoGuardaOTelefoneNemOIpEmTextoPuro() throws Exception {
        String telefone = telefoneUnico();
        String ip = ipUnico();
        errar(telefone, ip, 1);

        assertThat(authAttemptRepository.findAll()).extracting(AuthAttempt::getKeyHash)
                .doesNotContain(telefone, ip)
                .allSatisfy(hash -> assertThat(hash).hasSize(64));
    }
}
