package br.com.accessmap.backend.identity.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRequestDto {

    private String name;
    @Email(message = "Email inválido")
    private String email;
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    private String password;
    @Pattern(regexp = "\\d{10,11}", message = "Telefone inválido")
    private String phone;
    @Min(value = 0, message = "Idade inválida")
    @Max(value = 120, message = "Idade inválida")
    private Integer age;
    private String accessibilityNeeds;
}
