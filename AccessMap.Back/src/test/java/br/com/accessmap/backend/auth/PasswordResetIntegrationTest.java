package br.com.accessmap.backend.auth;

import br.com.accessmap.backend.AbstractIntegrationTest;
import br.com.accessmap.backend.auth.model.PasswordResetCode;
import br.com.accessmap.backend.auth.repository.PasswordResetCodeRepository;
import br.com.accessmap.backend.auth.repository.RefreshTokenRepository;
import br.com.accessmap.backend.auth.service.PasswordResetService;
import br.com.accessmap.backend.identity.repository.UserRepository;
import br.com.accessmap.backend.notification.MailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recuperação de senha por código no e-mail (#109). O envio é trocado por uma caixa de entrada em memória. */
@Import(PasswordResetIntegrationTest.CaixaDeEntradaConfig.class)
@TestPropertySource(properties = {
        "accessmap.auth.rate-limit.reset-per-ip=4",
        "accessmap.auth.rate-limit.reset-per-email=2"
})
class PasswordResetIntegrationTest extends AbstractIntegrationTest {

    private static final String SENHA = "senhaForte123";
    private static final String NOVA_SENHA = "outraSenha456";
    private static final AtomicInteger PROXIMO_IP = new AtomicInteger(1);
    private static final Pattern CODIGO = Pattern.compile("é: (\\d{6})");

    /** E-mails enviados, por destinatário. O envio é assíncrono: o teste espera a chegada. */
    static final Map<String, BlockingQueue<String>> CAIXA = new ConcurrentHashMap<>();

    @TestConfiguration
    static class CaixaDeEntradaConfig {
        @Bean
        @Primary
        MailSender caixaDeEntrada() {
            return (to, subject, text) -> CAIXA.computeIfAbsent(to, k -> new LinkedBlockingQueue<>()).add(text);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordResetCodeRepository codeRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    // ---------- helpers ----------

    private record Conta(String telefone, String email, String userId, String refreshToken) {
    }

    private String ipUnico() {
        return "10.1.0." + PROXIMO_IP.getAndIncrement();
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

    private Conta novaConta() throws Exception {
        String telefone = telefoneUnico();
        String email = emailUnico();
        String corpo = enviar("/api/auth/register", ipUnico(),
                Map.of("name", "Maria", "phone", telefone, "password", SENHA, "email", email))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode tokens = objectMapper.readTree(corpo);
        String userId = userRepository.findByPhone(telefone).orElseThrow().getId();
        return new Conta(telefone, email, userId, tokens.get("refreshToken").asString());
    }

    private ResultActions pedirCodigo(String email, String ip) throws Exception {
        return enviar("/api/auth/password-reset/request", ip, Map.of("email", email));
    }

    private ResultActions confirmar(String email, String codigo, String senha) throws Exception {
        return enviar("/api/auth/password-reset/confirm", ipUnico(),
                Map.of("email", email, "code", codigo, "password", senha));
    }

    private ResultActions login(String telefone, String senha) throws Exception {
        return enviar("/api/auth/login", ipUnico(), Map.of("phone", telefone, "password", senha));
    }

    /** Espera o e-mail chegar e devolve o código dele. */
    private String codigoRecebido(String email) throws InterruptedException {
        String texto = CAIXA.computeIfAbsent(email, k -> new LinkedBlockingQueue<>()).poll(5, TimeUnit.SECONDS);
        assertThat(texto).as("e-mail com o código para " + email).isNotNull();
        Matcher m = CODIGO.matcher(texto);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private String codigoErrado(String certo) {
        return certo.equals("000000") ? "111111" : "000000";
    }

    // ---------- fluxo completo ----------

    @Test
    void codigoValidoTrocaASenhaEEncerraAsSessoes() throws Exception {
        Conta conta = novaConta();

        pedirCodigo(conta.email(), ipUnico())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Se houver uma conta com esse e-mail, enviamos um código."));
        String codigo = codigoRecebido(conta.email());

        confirmar(conta.email(), codigo, NOVA_SENHA).andExpect(status().isOk());

        login(conta.telefone(), SENHA).andExpect(status().isUnauthorized());
        login(conta.telefone(), NOVA_SENHA).andExpect(status().isOk());
        enviar("/api/auth/refresh", ipUnico(), Map.of("refreshToken", conta.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emailEhEncontradoSemDiferenciarMaiusculas() throws Exception {
        Conta conta = novaConta();

        pedirCodigo(conta.email().toUpperCase(), ipUnico()).andExpect(status().isAccepted());
        String codigo = codigoRecebido(conta.email());

        confirmar(conta.email().toUpperCase(), codigo, NOVA_SENHA).andExpect(status().isOk());
    }

    @Test
    void emailSemContaRespondeIgualENaoEnviaNada() throws Exception {
        String email = emailUnico();

        pedirCodigo(email, ipUnico())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Se houver uma conta com esse e-mail, enviamos um código."));

        assertThat(CAIXA.computeIfAbsent(email, k -> new LinkedBlockingQueue<>()).poll(500, TimeUnit.MILLISECONDS))
                .isNull();
        confirmar(email, "123456", NOVA_SENHA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(PasswordResetService.CODIGO_INVALIDO));
    }

    // ---------- código recusado ----------

    @Test
    void codigoErradoEhRecusado() throws Exception {
        Conta conta = novaConta();
        pedirCodigo(conta.email(), ipUnico()).andExpect(status().isAccepted());
        String codigo = codigoRecebido(conta.email());

        confirmar(conta.email(), codigoErrado(codigo), NOVA_SENHA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(PasswordResetService.CODIGO_INVALIDO));
        login(conta.telefone(), SENHA).andExpect(status().isOk());
    }

    @Test
    void codigoExpiradoEhRecusado() throws Exception {
        Conta conta = novaConta();
        pedirCodigo(conta.email(), ipUnico()).andExpect(status().isAccepted());
        String codigo = codigoRecebido(conta.email());

        List<PasswordResetCode> codigos = codeRepository.findAll().stream()
                .filter(c -> c.getUserId().equals(conta.userId())).toList();
        codigos.forEach(c -> c.setExpiresAt(c.getCreatedAt().minusMinutes(1)));
        codeRepository.saveAll(codigos);

        confirmar(conta.email(), codigo, NOVA_SENHA).andExpect(status().isBadRequest());
    }

    @Test
    void codigoJaUsadoNaoServeDeNovo() throws Exception {
        Conta conta = novaConta();
        pedirCodigo(conta.email(), ipUnico()).andExpect(status().isAccepted());
        String codigo = codigoRecebido(conta.email());

        confirmar(conta.email(), codigo, NOVA_SENHA).andExpect(status().isOk());
        confirmar(conta.email(), codigo, "terceiraSenha789").andExpect(status().isBadRequest());
        login(conta.telefone(), NOVA_SENHA).andExpect(status().isOk());
    }

    @Test
    void pedirDeNovoInvalidaOCodigoAnterior() throws Exception {
        Conta conta = novaConta();
        pedirCodigo(conta.email(), ipUnico()).andExpect(status().isAccepted());
        String primeiro = codigoRecebido(conta.email());
        pedirCodigo(conta.email(), ipUnico()).andExpect(status().isAccepted());
        String segundo = codigoRecebido(conta.email());

        if (!primeiro.equals(segundo)) {
            confirmar(conta.email(), primeiro, NOVA_SENHA).andExpect(status().isBadRequest());
        }
        confirmar(conta.email(), segundo, NOVA_SENHA).andExpect(status().isOk());
    }

    @Test
    void quintaTentativaErradaInvalidaAteOCodigoCerto() throws Exception {
        Conta conta = novaConta();
        pedirCodigo(conta.email(), ipUnico()).andExpect(status().isAccepted());
        String codigo = codigoRecebido(conta.email());

        for (int i = 0; i < 5; i++) {
            confirmar(conta.email(), codigoErrado(codigo), NOVA_SENHA).andExpect(status().isBadRequest());
        }

        confirmar(conta.email(), codigo, NOVA_SENHA).andExpect(status().isBadRequest());
        login(conta.telefone(), SENHA).andExpect(status().isOk());
    }

    @Test
    void senhaNovaSegueAsMesmasRegras() throws Exception {
        Conta conta = novaConta();
        pedirCodigo(conta.email(), ipUnico()).andExpect(status().isAccepted());
        String codigo = codigoRecebido(conta.email());

        confirmar(conta.email(), codigo, "curta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.password").exists());
        // A senha recusada pela validação não gasta o código
        confirmar(conta.email(), codigo, NOVA_SENHA).andExpect(status().isOk());
    }

    @Test
    void naoGuardaOCodigoEmTextoPuro() throws Exception {
        Conta conta = novaConta();
        pedirCodigo(conta.email(), ipUnico()).andExpect(status().isAccepted());
        String codigo = codigoRecebido(conta.email());

        assertThat(codeRepository.findAll())
                .filteredOn(c -> c.getUserId().equals(conta.userId()))
                .isNotEmpty()
                .allSatisfy(c -> assertThat(c.getCodeHash()).doesNotContain(codigo).startsWith("$2"));
    }

    // ---------- limite de pedidos ----------

    @Test
    void limitaPedidosPorEmail() throws Exception {
        Conta conta = novaConta();

        pedirCodigo(conta.email(), ipUnico()).andExpect(status().isAccepted());
        pedirCodigo(conta.email(), ipUnico()).andExpect(status().isAccepted());
        pedirCodigo(conta.email(), ipUnico()).andExpect(status().isTooManyRequests());
    }

    @Test
    void limitaPedidosPorIp() throws Exception {
        String ip = ipUnico();

        for (int i = 0; i < 4; i++) {
            pedirCodigo(emailUnico(), ip).andExpect(status().isAccepted());
        }
        pedirCodigo(emailUnico(), ip).andExpect(status().isTooManyRequests());
    }
}
