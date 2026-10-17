package br.com.accessmap.backend.review.dto;

import br.com.accessmap.backend.review.enums.AccessibilityTag;
import br.com.accessmap.backend.review.enums.TagAssessment;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ReviewUpdateRequestDto {

    @Min(value = 1, message = "rating deve ser entre 1 e 5")
    @Max(value = 5, message = "rating deve ser entre 1 e 5")
    private Integer rating;

    private String comment;

    private Map<AccessibilityTag, @NotNull(message = "Avaliação da tag é obrigatória") TagAssessment> tags;
}
