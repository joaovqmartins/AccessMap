package br.com.accessmap.backend.identity.dto;

import br.com.accessmap.backend.identity.enums.AccessibilityNeed;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Set;

@Data
public class UserRequestDto {

    private String name;
    @Email(message = "Email inválido")
    private String email;
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    private String password;
    private String currentPassword;
    @Pattern(regexp = "\\d{10,11}", message = "Telefone inválido")
    private String phone;
    @Min(value = 0, message = "Idade inválida")
    @Max(value = 120, message = "Idade inválida")
    private Integer age;
    private Set<AccessibilityNeed> accessibilityNeeds;
}
