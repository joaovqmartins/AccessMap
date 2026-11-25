package br.com.accessmap.backend.auth.dto;

import br.com.accessmap.backend.validation.FieldLimits;
import br.com.accessmap.backend.validation.MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetConfirmDto {

    @NotBlank(message = "email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = FieldLimits.TEXT_MAX, message = "E-mail deve ter no máximo 255 caracteres")
    private String email;

    @NotBlank(message = "code é obrigatório")
    @Pattern(regexp = "\\d{6}", message = "O código tem 6 números")
    private String code;

    @NotBlank(message = "password é obrigatório")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    @MaxBytes(value = FieldLimits.PASSWORD_MAX_BYTES, message = FieldLimits.PASSWORD_TOO_LONG)
    private String password;
}
