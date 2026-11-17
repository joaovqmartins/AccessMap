package br.com.accessmap.backend.identity.dto;

import br.com.accessmap.backend.identity.enums.AccessibilityNeed;
import br.com.accessmap.backend.validation.FieldLimits;
import br.com.accessmap.backend.validation.MaxBytes;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Set;

@Data
public class UserRequestDto {

    @Size(max = FieldLimits.TEXT_MAX, message = "Nome deve ter no máximo 255 caracteres")
    private String name;
    @Email(message = "Email inválido")
    @Size(max = FieldLimits.TEXT_MAX, message = "E-mail deve ter no máximo 255 caracteres")
    private String email;
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    @MaxBytes(value = FieldLimits.PASSWORD_MAX_BYTES, message = FieldLimits.PASSWORD_TOO_LONG)
    private String password;
    @MaxBytes(value = FieldLimits.PASSWORD_MAX_BYTES, message = FieldLimits.PASSWORD_TOO_LONG)
    private String currentPassword;
    @Pattern(regexp = "\\d{10,11}", message = "Telefone inválido")
    private String phone;
    @Min(value = 0, message = "Idade inválida")
    @Max(value = 120, message = "Idade inválida")
    private Integer age;
    private Set<AccessibilityNeed> accessibilityNeeds;
}
