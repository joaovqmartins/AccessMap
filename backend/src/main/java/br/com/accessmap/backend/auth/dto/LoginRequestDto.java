package br.com.accessmap.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {

    @NotBlank(message = "email é obrigatório")
    private String email;

    @NotBlank(message = "password é obrigatório")
    private String password;
}
