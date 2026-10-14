package br.com.accessmap.backend.auth;

import br.com.accessmap.backend.AbstractIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsIntegrationTest {

    private static final String ORIGEM_WEB = "http://localhost:8081";

    @Nested
    class SemOrigensConfiguradas extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void naoDevolveCabecalhosDeCors() throws Exception {
            mockMvc.perform(get("/api/places").header(HttpHeaders.ORIGIN, ORIGEM_WEB))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }

    @Nested
    @TestPropertySource(properties = "accessmap.cors.allowed-origins=" + ORIGEM_WEB)
    class ComOrigemConfigurada extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void preflightDaOrigemLiberadaPassaSemToken() throws Exception {
            mockMvc.perform(options("/api/users/me")
                            .header(HttpHeaders.ORIGIN, ORIGEM_WEB)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_WEB));
        }

        @Test
        void requisicaoDaOrigemLiberadaRecebeCabecalho() throws Exception {
            mockMvc.perform(get("/api/places").header(HttpHeaders.ORIGIN, ORIGEM_WEB))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_WEB));
        }

        @Test
        void origemNaoLiberadaEhRecusada() throws Exception {
            mockMvc.perform(get("/api/places").header(HttpHeaders.ORIGIN, "http://evil.example"))
                    .andExpect(status().isForbidden());
        }
    }
}
