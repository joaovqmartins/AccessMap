package br.com.accessmap.backend.place;

import br.com.accessmap.backend.AbstractIntegrationTest;
import br.com.accessmap.backend.auth.service.TokenService;
import br.com.accessmap.backend.identity.enums.Role;
import br.com.accessmap.backend.identity.model.User;
import br.com.accessmap.backend.identity.repository.UserRepository;
import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.repository.PlaceRepository;
import br.com.accessmap.backend.review.enums.ReviewStatus;
import br.com.accessmap.backend.review.enums.SuspicionReason;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaceWeightedScoreIntegrationTest {

    /** Monta usuários e avaliações; as classes abaixo só mudam a configuração de pesos. */
    abstract static class Base extends AbstractIntegrationTest {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        ObjectMapper objectMapper;

        @Autowired
        UserRepository userRepository;

        @Autowired
        ReviewRepository reviewRepository;

        @Autowired
        PlaceRepository placeRepository;

        @Autowired
        PasswordEncoder passwordEncoder;

        @Autowired
        TokenService tokenService;

        User criarUsuario(Role role, LocalDateTime criadoEm) {
            return userRepository.save(User.builder()
                    .name("Pessoa")
                    .phone(telefoneUnico())
                    .password(passwordEncoder.encode("senhaForte123"))
                    .role(role)
                    .accessibilityNeeds(Set.of())
                    .createdAt(criadoEm)
                    .updatedAt(criadoEm)
                    .build());
        }

        User contaAntiga() {
            return criarUsuario(Role.USER, LocalDateTime.now().minusDays(40));
        }

        User contaNova() {
            return criarUsuario(Role.USER, LocalDateTime.now());
        }

        /** Conta com histórico de 5 reviews publicadas: a próxima sai como Confiável. */
        User contaConfiavel() {
            User user = contaAntiga();
            LocalDateTime criadaEm = LocalDateTime.now().minusDays(20);
            for (int i = 0; i < 5; i++) {
                reviewRepository.save(Review.builder()
                        .userId(user.getId())
                        .placeId(placeUnico())
                        .rating(4)
                        .createdAt(criadaEm)
                        .updatedAt(criadaEm)
                        .build());
            }
            return user;
        }

        MockHttpServletRequestBuilder como(User user, MockHttpServletRequestBuilder requisicao) {
            return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.issueAccessToken(user));
        }

        String placeUnico() {
            return "ChIJ-" + UUID.randomUUID();
        }

        String avaliar(User autor, String placeId, int nota) throws Exception {
            String corpo = mockMvc.perform(como(autor, post("/api/reviews"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "placeId", placeId,
                                    "rating", nota,
                                    "comment", "Teste de nota ponderada.",
                                    "tags", Map.of("RAMPAS_E_ENTRADAS", "ADEQUADO")))))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            return objectMapper.readTree(corpo).get("id").asString();
        }

        double media(String placeId) {
            return placeRepository.findByPlaceId(placeId).orElseThrow().getAverageScore();
        }
    }

    @Nested
    class ComOsPesosPadrao extends Base {

        @Test
        void exemploDaPropostaDaQuatro() throws Exception {
            String placeId = placeUnico();
            avaliar(contaConfiavel(), placeId, 5);
            avaliar(contaAntiga(), placeId, 4);
            avaliar(contaNova(), placeId, 1);

            assertThat(media(placeId)).isCloseTo(4.0, within(0.0001));
        }

        @Test
        void reviewCountContaAvaliacoesSemPeso() throws Exception {
            String placeId = placeUnico();
            avaliar(contaConfiavel(), placeId, 5);
            avaliar(contaNova(), placeId, 1);

            mockMvc.perform(get("/api/places/" + placeId)).andExpect(jsonPath("$.reviewCount").value(2));
        }

        @Test
        void votosPorCaracteristicaContinuamInteiros() throws Exception {
            String placeId = placeUnico();
            avaliar(contaConfiavel(), placeId, 5);
            avaliar(contaNova(), placeId, 1);

            mockMvc.perform(get("/api/places/" + placeId))
                    .andExpect(jsonPath("$.tagStats.RAMPAS_E_ENTRADAS.adequadoCount").value(2));
        }

        @Test
        void reviewOcultaFicaForaDaMedia() throws Exception {
            String placeId = placeUnico();
            String oculta = avaliar(contaAntiga(), placeId, 5);
            avaliar(contaNova(), placeId, 2);

            for (int i = 0; i < 3; i++) {
                mockMvc.perform(como(contaAntiga(), post("/api/reviews/" + oculta + "/report"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("reason", "SPAM"))))
                        .andExpect(status().isCreated());
            }

            assertThat(media(placeId)).isCloseTo(2.0, within(0.0001));
        }

        @Test
        void aBuscaOrdenaPelaMediaPonderada() throws Exception {
            // Sem peso, as médias empatam em 3,0. Com peso, a nota alta da conta confiável puxa o primeiro local.
            String sobe = placeUnico(), desce = placeUnico();
            avaliar(contaConfiavel(), sobe, 5);
            avaliar(contaNova(), sobe, 1);
            avaliar(contaNova(), desce, 5);
            avaliar(contaConfiavel(), desce, 1);

            String corpo = mockMvc.perform(get("/api/places?size=1000"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> ids = new ArrayList<>();
            objectMapper.readTree(corpo).get("content").forEach((JsonNode lugar) -> ids.add(lugar.get("placeId").asString()));

            assertThat(ids.indexOf(sobe)).isLessThan(ids.indexOf(desce));
        }

        @Test
        void editarParaNotaDestoanteUsaAMediaPonderadaDosOutros() throws Exception {
            // Média dos outros: 2,0, então a nota 5 está a 3 pontos. Reconstruir pela média do local daria 2,09 e não sinalizaria.
            String placeId = placeUnico();
            for (int i = 0; i < 5; i++) {
                avaliar(contaAntiga(), placeId, 2);
            }
            User nova = contaNova();
            String reviewId = avaliar(nova, placeId, 1);

            mockMvc.perform(como(nova, patch("/api/reviews/" + reviewId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("rating", 5))))
                    .andExpect(status().isOk());

            Review editada = reviewRepository.findById(reviewId).orElseThrow();
            assertThat(editada.getSuspicionReason()).isEqualTo(SuspicionReason.NOTA_DESTOANTE);
            assertThat(editada.getStatus()).isEqualTo(ReviewStatus.PUBLICADA);
        }

        @Test
        void recalcularCorrigeANotaGuardadaEPodeRodarDeNovo() throws Exception {
            String placeId = placeUnico();
            avaliar(contaAntiga(), placeId, 4);
            Place place = placeRepository.findByPlaceId(placeId).orElseThrow();
            place.setAverageScore(1.0);
            placeRepository.save(place);
            User admin = criarUsuario(Role.ADMIN, LocalDateTime.now().minusDays(40));

            mockMvc.perform(como(admin, post("/api/admin/places/recalculate")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recalculated").isNumber());
            assertThat(media(placeId)).isCloseTo(4.0, within(0.0001));

            mockMvc.perform(como(admin, post("/api/admin/places/recalculate"))).andExpect(status().isOk());
            assertThat(media(placeId)).isCloseTo(4.0, within(0.0001));
        }

        @Test
        void recalcularExigeAdmin() throws Exception {
            mockMvc.perform(post("/api/admin/places/recalculate")).andExpect(status().isUnauthorized());
            mockMvc.perform(como(contaAntiga(), post("/api/admin/places/recalculate"))).andExpect(status().isForbidden());
        }
    }

    @Nested
    @TestPropertySource(properties = "accessmap.review.trust.weight-new=3.0")
    class ComOutroPesoNaConfiguracao extends Base {

        @Test
        void oPesoVemDaConfiguracao() throws Exception {
            String placeId = placeUnico();
            avaliar(contaAntiga(), placeId, 5);
            avaliar(contaNova(), placeId, 1);

            assertThat(media(placeId)).isCloseTo(2.0, within(0.0001));
        }
    }
}
