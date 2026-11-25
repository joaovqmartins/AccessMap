package br.com.accessmap.backend.auth.dto;

import br.com.accessmap.backend.validation.FieldLimits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetRequestDto {

    @NotBlank(message = "email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = FieldLimits.TEXT_MAX, message = "E-mail deve ter no máximo 255 caracteres")
    private String email;
}
