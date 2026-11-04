package br.com.accessmap.backend.review.dto;

import br.com.accessmap.backend.review.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewReportRequestDto {

    @NotNull(message = "reason é obrigatório")
    private ReportReason reason;
}
