package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.review.enums.ReportReason;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.model.ReviewReport;
import br.com.accessmap.backend.review.repository.ReviewReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewReportServiceTest {

    @Mock
    private ReviewReportRepository reviewReportRepository;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewReportService reviewReportService;

    private void avaliacaoDe(String autorId) {
        when(reviewService.findById("r1")).thenReturn(Review.builder().id("r1").userId(autorId).build());
    }

    @Test
    void deveRegistrarDenunciaVinculadaAQuemDenunciou() {
        avaliacaoDe("autor");

        reviewReportService.report("r1", "denunciante", ReportReason.SPAM);

        ArgumentCaptor<ReviewReport> salva = ArgumentCaptor.forClass(ReviewReport.class);
        verify(reviewReportRepository).saveAndFlush(salva.capture());
        assertThat(salva.getValue().getReviewId()).isEqualTo("r1");
        assertThat(salva.getValue().getReporterId()).isEqualTo("denunciante");
        assertThat(salva.getValue().getReason()).isEqualTo(ReportReason.SPAM);
        assertThat(salva.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void naoDeveDeixarDenunciarAPropriaAvaliacao() {
        avaliacaoDe("autor");

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> reviewReportService.report("r1", "autor", ReportReason.OUTRO));

        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(reviewReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveBloquearDenunciaDuplicadaDoMesmoUsuario() {
        avaliacaoDe("autor");
        when(reviewReportRepository.existsByReviewIdAndReporterId("r1", "denunciante")).thenReturn(true);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> reviewReportService.report("r1", "denunciante", ReportReason.SPAM));

        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(erro.getReason()).isEqualTo("Você já denunciou esta avaliação");
        verify(reviewReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void envioSimultaneoBarradoPeloIndiceUnicoViraConflito() {
        avaliacaoDe("autor");
        when(reviewReportRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicada"));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> reviewReportService.report("r1", "denunciante", ReportReason.SPAM));

        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void avaliacaoInexistenteOuNaoPublicadaPropagaO404SemRegistrar() {
        when(reviewService.findById("r1")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> reviewReportService.report("r1", "denunciante", ReportReason.SPAM));

        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(reviewReportRepository, never()).saveAndFlush(any());
    }
}
