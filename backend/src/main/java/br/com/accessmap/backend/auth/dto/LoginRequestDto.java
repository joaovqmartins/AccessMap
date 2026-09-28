package br.com.accessmap.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {

    @NotBlank(message = "phone é obrigatório")
    private String phone;

    @NotBlank(message = "password é obrigatório")
    private String password;
}
