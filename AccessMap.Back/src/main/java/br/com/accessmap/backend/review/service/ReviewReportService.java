package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.review.enums.ReportReason;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.model.ReviewReport;
import br.com.accessmap.backend.review.repository.ReviewReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewReportService {

    private static final String JA_DENUNCIADA = "Você já denunciou esta avaliação";

    private final ReviewReportRepository reviewReportRepository;
    private final ReviewService reviewService;

    /**
     * Só registra a denúncia: ocultar a avaliação depende das regras da #27.
     * Sem {@code @Transactional} de propósito: a violação do índice único precisa estourar aqui,
     * no {@code saveAndFlush}, para virar 409, e não no commit de uma transação externa.
     *
     * @param reporterId quem denuncia, extraído do token — nunca do corpo da requisição
     */
    public void report(String reviewId, String reporterId, ReportReason reason) {
        Review review = reviewService.findById(reviewId);

        if (review.getUserId().equals(reporterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode denunciar a própria avaliação");
        }
        if (reviewReportRepository.existsByReviewIdAndReporterId(reviewId, reporterId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, JA_DENUNCIADA);
        }

        try {
            reviewReportRepository.saveAndFlush(ReviewReport.builder()
                    .reviewId(reviewId)
                    .reporterId(reporterId)
                    .reason(reason)
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            // Dois envios simultâneos passam juntos pela checagem acima; o índice único barra o segundo
            throw new ResponseStatusException(HttpStatus.CONFLICT, JA_DENUNCIADA);
        }
    }
}
