package br.com.accessmap.backend.auth;

import br.com.accessmap.backend.AbstractIntegrationTest;
import br.com.accessmap.backend.auth.model.RefreshToken;
import br.com.accessmap.backend.auth.repository.RefreshTokenRepository;
import br.com.accessmap.backend.auth.service.RefreshTokenCleanupJob;
import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RefreshTokenCleanupIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenCleanupJob cleanupJob;

    private JsonNode registrar(String telefone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Maria", "phone", telefone, "password", "senhaForte123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode refresh(String refreshToken, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private RefreshToken salvar(String userId, LocalDateTime expiresAt, LocalDateTime revokedAt) {
        return refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(tokenService.hash(UUID.randomUUID().toString()))
                .userId(userId)
                .expiresAt(expiresAt)
                .revokedAt(revokedAt)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build());
    }

    @Test
    void limpezaApagaExpiradosERevogadosAntigosEMantemAtivosERevogadosRecentes() throws Exception {
        String telefone = telefoneUnico();
        String refresh1 = registrar(telefone).get("refreshToken").asString();
        String userId = userRepository.findByPhone(telefone).orElseThrow().getId();
        LocalDateTime agora = LocalDateTime.now();

        // refresh1 vira o revogado recente (rotacionado há 1 hora); refresh2 é o ativo
        String refresh2 = refresh(refresh1, 200).get("refreshToken").asString();
        RefreshToken revogadoRecente = refreshTokenRepository.findByTokenHash(tokenService.hash(refresh1)).orElseThrow();
        revogadoRecente.setRevokedAt(agora.minusHours(1));
        refreshTokenRepository.save(revogadoRecente);
        RefreshToken ativo = refreshTokenRepository.findByTokenHash(tokenService.hash(refresh2)).orElseThrow();

        RefreshToken expirado = salvar(userId, agora.minusDays(1), null);
        RefreshToken revogadoAntigo = salvar(userId, agora.plusDays(1), agora.minusDays(8));

        cleanupJob.cleanUp();

        assertThat(refreshTokenRepository.existsById(expirado.getId())).isFalse();
        assertThat(refreshTokenRepository.existsById(revogadoAntigo.getId())).isFalse();
        assertThat(refreshTokenRepository.existsById(ativo.getId())).isTrue();
        assertThat(refreshTokenRepository.existsById(revogadoRecente.getId())).isTrue();

        // o revogado recente continua detectando reuso: reapresentá-lo derruba todas as sessões...
        JsonNode erro = refresh(refresh1, 401);
        assertThat(erro.get("mensagem").asString()).isEqualTo("Sessão inválida. Faça login novamente.");

        // ...inclusive a do refresh2, que era válido
        refresh(refresh2, 401);
    }
}
