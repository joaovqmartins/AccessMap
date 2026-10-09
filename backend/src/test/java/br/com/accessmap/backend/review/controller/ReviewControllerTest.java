package br.com.accessmap.backend.review.controller;

import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.service.ReviewService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Só a escolha da consulta em {@code GET /api/reviews}; a regra de cada consulta fica no service/repositório.
 * MockMvc standalone: sem contexto Spring, sem banco, sem Docker.
 */
@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController controller;

    private MockMvc mockMvc;

    private final Page<Review> pagina = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

    @BeforeEach
    void montarMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void soPlaceIdListaAsAvaliacoesDoLocal() throws Exception {
        when(reviewService.findByPlaceId(eq("p1"), any(Pageable.class))).thenReturn(pagina);

        mockMvc.perform(get("/api/reviews?placeId=p1")).andExpect(status().isOk());

        verify(reviewService).findByPlaceId(eq("p1"), any(Pageable.class));
        verifyNoMoreInteractions(reviewService);
    }

    @Test
    void soUserIdListaAsAvaliacoesDoUsuario() throws Exception {
        when(reviewService.findByUserId(eq("u1"), any(Pageable.class))).thenReturn(pagina);

        mockMvc.perform(get("/api/reviews?userId=u1")).andExpect(status().isOk());

        verify(reviewService).findByUserId(eq("u1"), any(Pageable.class));
        verifyNoMoreInteractions(reviewService);
    }

    @Test
    void placeIdEUserIdJuntosFiltramPelosDois() throws Exception {
        when(reviewService.findByUserIdAndPlaceId(eq("u1"), eq("p1"), any(Pageable.class))).thenReturn(pagina);

        mockMvc.perform(get("/api/reviews?placeId=p1&userId=u1")).andExpect(status().isOk());

        verify(reviewService).findByUserIdAndPlaceId(eq("u1"), eq("p1"), any(Pageable.class));
        verifyNoMoreInteractions(reviewService);
    }

    @Test
    void semFiltrosListaTodasAsPublicadas() throws Exception {
        when(reviewService.findAll(any(Pageable.class))).thenReturn(pagina);

        mockMvc.perform(get("/api/reviews")).andExpect(status().isOk());

        verify(reviewService).findAll(any(Pageable.class));
        verifyNoMoreInteractions(reviewService);
    }

    @Test
    void valoresEmBrancoSaoTratadosComoAusentes() throws Exception {
        when(reviewService.findAll(any(Pageable.class))).thenReturn(pagina);

        mockMvc.perform(get("/api/reviews").param("placeId", "  ").param("userId", "")).andExpect(status().isOk());

        verify(reviewService).findAll(any(Pageable.class));
        verifyNoMoreInteractions(reviewService);
    }
}
