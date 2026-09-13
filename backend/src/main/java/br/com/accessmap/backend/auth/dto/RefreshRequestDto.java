package br.com.accessmap.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequestDto {

    @NotBlank(message = "refreshToken é obrigatório")
    private String refreshToken;
}
