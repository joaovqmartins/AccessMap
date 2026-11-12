package br.com.accessmap.backend.auth.dto;

import br.com.accessmap.backend.validation.FieldLimits;
import br.com.accessmap.backend.validation.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {

    @NotBlank(message = "phone é obrigatório")
    private String phone;

    @NotBlank(message = "password é obrigatório")
    @MaxBytes(value = FieldLimits.PASSWORD_MAX_BYTES, message = FieldLimits.PASSWORD_TOO_LONG)
    private String password;
}
