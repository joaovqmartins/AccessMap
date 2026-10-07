package br.com.accessmap.backend.place.dto;

import br.com.accessmap.backend.review.enums.AccessibilityTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class PlaceBatchRequestDto {

    public static final int MAX_PLACE_IDS = 60;

    @NotEmpty(message = "Informe ao menos um placeId")
    @Size(max = MAX_PLACE_IDS, message = "Informe no máximo " + MAX_PLACE_IDS + " placeIds por chamada")
    private List<@NotBlank(message = "placeId não pode ser vazio") String> placeIds;

    /** Opcional: se vier, só voltam os locais que têm todas essas características. */
    private Set<AccessibilityTag> tags;
}
