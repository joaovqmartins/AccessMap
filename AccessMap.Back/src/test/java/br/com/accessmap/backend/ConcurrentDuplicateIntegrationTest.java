package br.com.accessmap.backend;

import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import br.com.accessmap.backend.identity.service.UserService;
import br.com.accessmap.backend.place.service.PlaceService;
import br.com.accessmap.backend.review.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Envio duplicado simultâneo (toque duplo, reenvio do app): as duas requisições podem passar juntas
 * pela checagem do serviço e o banco barra a segunda pela constraint. A resposta tem de ser 409,
 * igual à da checagem do serviço, nunca 500.
 */
class ConcurrentDuplicateIntegrationTest extends AbstractIntegrationTest {

    private static final int RODADAS = 10;

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

    @Autowired
    private PlaceService placeService;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void encerrarExecutor() {
        executor.shutdownNow();
    }

    /** Dispara duas cópias da requisição soltas pelo mesmo latch e devolve as respostas. */
    private List<MockHttpServletResponse> simultaneas(Supplier<RequestBuilder> requisicao) throws Exception {
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<MockHttpServletResponse>> futuros = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            RequestBuilder builder = requisicao.get();
            futuros.add(executor.submit(() -> {
                prontas.countDown();
                largada.await();
                return mockMvc.perform(builder).andReturn().getResponse();
            }));
        }
        assertThat(prontas.await(10, TimeUnit.SECONDS)).isTrue();
        largada.countDown();

        List<MockHttpServletResponse> respostas = new ArrayList<>();
        for (Future<MockHttpServletResponse> futuro : futuros) {
            respostas.add(futuro.get(30, TimeUnit.SECONDS));
        }
        return respostas;
    }

    private void assertUmCriadoEUmConflito(List<MockHttpServletResponse> respostas, String mensagem) throws Exception {
        List<Integer> status = respostas.stream().map(MockHttpServletResponse::getStatus).toList();
        assertThat(status).containsExactlyInAnyOrder(201, 409);

        MockHttpServletResponse conflito = respostas.stream()
                .filter(r -> r.getStatus() == 409).findFirst().orElseThrow();
        JsonNode corpo = objectMapper.readTree(conflito.getContentAsString());
        assertThat(corpo.get("status").asInt()).isEqualTo(409);
        assertThat(corpo.get("mensagem").asString()).isEqualTo(mensagem);
    }

    @Test
    void cadastroDuplicadoSimultaneoRespondeConflito() throws Exception {
        for (int rodada = 0; rodada < RODADAS; rodada++) {
            String corpo = objectMapper.writeValueAsString(Map.of(
                    "name", "Maria",
                    "phone", telefoneUnico(),
                    "password", "senhaForte123"
            ));

            List<MockHttpServletResponse> respostas = simultaneas(() -> post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo));

            assertUmCriadoEUmConflito(respostas, UserService.TELEFONE_EM_USO);
        }
    }

    @Test
    void avaliacaoDuplicadaSimultaneaRespondeConflito() throws Exception {
        for (int rodada = 0; rodada < RODADAS; rodada++) {
            User autor = userRepository.save(User.builder()
                    .name("Pessoa")
                    .phone(telefoneUnico())
                    .password(passwordEncoder.encode("senhaForte123"))
                    .role(Role.USER)
                    .accessibilityNeeds(Set.of())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
            String bearer = "Bearer " + tokenService.issueAccessToken(autor);

            // O local já existe: a colisão em uk_places_place_id (local novo avaliado ao mesmo tempo) é outro caso
            String placeId = "ChIJ-" + UUID.randomUUID();
            placeService.findOrCreateByPlaceId(placeId);

            String corpo = objectMapper.writeValueAsString(Map.of(
                    "placeId", placeId,
                    "rating", 4,
                    "comment", "Rampa boa.",
                    "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")
            ));

            List<MockHttpServletResponse> respostas = simultaneas(() -> post("/api/reviews")
                    .header(HttpHeaders.AUTHORIZATION, bearer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo));

            assertUmCriadoEUmConflito(respostas, ReviewService.LOCAL_JA_AVALIADO);
        }
    }
}
