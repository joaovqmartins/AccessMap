package br.com.accessmap.backend.place.controller;

import br.com.accessmap.backend.exception.ApiExceptionHandler;
import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.place.service.PlaceService;
import br.com.accessmap.backend.review.enums.AccessibilityTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validação dos parâmetros e do corpo dos endpoints de busca, com o {@link ApiExceptionHandler} real.
 * MockMvc standalone: sem contexto Spring, sem banco, sem Docker.
 */
@ExtendWith(MockitoExtension.class)
class PlaceControllerTest {

    @Mock
    private PlaceService placeService;

    @InjectMocks
    private PlaceController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void montarMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void tagInexistenteNaBuscaDevolve400ListandoOsValoresAceitos() throws Exception {
        mockMvc.perform(get("/api/places?tags=BANANA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value(containsString("BANANA")))
                .andExpect(jsonPath("$.mensagem").value(containsString("RAMPAS_E_ENTRADAS")));

        verifyNoInteractions(placeService);
    }

    @Test
    void umaTagInvalidaEntreValidasTambemDevolve400() throws Exception {
        mockMvc.perform(get("/api/places?tags=ELEVADORES,BANANA"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(placeService);
    }

    @Test
    void tagsSeparadasPorVirgulaChegamAoServicoComoConjunto() throws Exception {
        Page<Place> vazia = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(placeService.search(any(), any(Pageable.class))).thenReturn(vazia);

        mockMvc.perform(get("/api/places?tags=ELEVADORES,RAMPAS_E_ENTRADAS"))
                .andExpect(status().isOk());

        verify(placeService).search(
                eq(Set.of(AccessibilityTag.ELEVADORES, AccessibilityTag.RAMPAS_E_ENTRADAS)), any(Pageable.class));
    }

    @Test
    void buscaEmLoteComListaVaziaDevolve400ComOCampo() throws Exception {
        mockMvc.perform(post("/api/places/batch").contentType(MediaType.APPLICATION_JSON).content("{\"placeIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.placeIds").exists());

        verifyNoInteractions(placeService);
    }

    @Test
    void buscaEmLoteComTagInexistenteNoCorpoDevolve400ListandoOsValoresAceitos() throws Exception {
        mockMvc.perform(post("/api/places/batch").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placeIds\":[\"x\"],\"tags\":[\"BANANA\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("BANANA")))
                .andExpect(jsonPath("$.mensagem").value(containsString("ELEVADORES")));

        verifyNoInteractions(placeService);
    }
}
