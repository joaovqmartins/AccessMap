package br.com.accessmap.backend;

import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Health check para a plataforma de deploy (#132): público só o status, detalhes só para o ADMIN. */
class HealthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    private String bearer(Role role) {
        LocalDateTime agora = LocalDateTime.now();
        User user = userRepository.save(User.builder()
                .name("Pessoa")
                .phone(telefoneUnico())
                .password("x")
                .role(role)
                .accessibilityNeeds(Set.of())
                .createdAt(agora)
                .updatedAt(agora)
                .build());
        return "Bearer " + tokenService.issueAccessToken(user);
    }

    @Test
    void healthLivenessEReadinessRespondemUpSemLogin() throws Exception {
        for (String caminho : new String[]{"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"}) {
            mockMvc.perform(get(caminho))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    @Test
    void semLoginOHealthNaoMostraDetalhes() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void usuarioComumTambemNaoVeDetalhes() throws Exception {
        mockMvc.perform(get("/actuator/health").header(HttpHeaders.AUTHORIZATION, bearer(Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void adminVeOBancoNosDetalhes() throws Exception {
        mockMvc.perform(get("/actuator/health").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.db.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness").header(HttpHeaders.AUTHORIZATION, bearer(Role.ADMIN)))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    @Test
    void outrosEndpointsDoActuatorNaoFicamPublicos() throws Exception {
        for (String caminho : new String[]{"/actuator", "/actuator/env", "/actuator/info", "/actuator/metrics",
                "/actuator/beans", "/actuator/loggers"}) {
            mockMvc.perform(get(caminho))
                    .andExpect(status().is(anyOf(is(401), is(404))));
        }
    }

    @Test
    void nemOAdminAcessaOsEndpointsNaoExpostos() throws Exception {
        String admin = bearer(Role.ADMIN);
        for (String caminho : new String[]{"/actuator/env", "/actuator/metrics", "/actuator/beans"}) {
            mockMvc.perform(get(caminho).header(HttpHeaders.AUTHORIZATION, admin))
                    .andExpect(status().isNotFound());
        }
    }
}
