package br.com.accessmap.backend.auth.dto;

import br.com.accessmap.backend.identity.enums.AccessibilityNeed;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

/** Cadastro simplificado: telefone, senha e nome. O resto é opcional e pode ser completado no perfil. */
@Data
public class RegisterRequestDto {

    @NotBlank(message = "phone é obrigatório")
    @Pattern(regexp = "\\d{10,11}", message = "Telefone inválido")
    private String phone;

    @NotBlank(message = "password é obrigatório")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    private String password;

    @NotBlank(message = "name é obrigatório")
    private String name;

    @Email(message = "Email inválido")
    private String email;

    @Min(value = 1, message = "Idade inválida")
    @Max(value = 120, message = "Idade inválida")
    private Integer age;

    private Set<AccessibilityNeed> accessibilityNeeds;
}
