package br.com.accessmap.backend.identity.dto;

import br.com.accessmap.backend.validation.FieldLimits;
import br.com.accessmap.backend.validation.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Excluir a própria conta pede a senha: um token esquecido aberto não basta para apagar a conta. */
@Data
public class DeleteAccountRequestDto {

    @NotBlank(message = "Informe a senha atual")
    @MaxBytes(value = FieldLimits.PASSWORD_MAX_BYTES, message = FieldLimits.PASSWORD_TOO_LONG)
    private String currentPassword;
}
