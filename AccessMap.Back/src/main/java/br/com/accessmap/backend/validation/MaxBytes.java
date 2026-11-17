package br.com.accessmap.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limita o tamanho do texto em bytes UTF-8, não em caracteres como o {@code @Size}.
 * Necessário para a senha: o BCrypt só usa 72 bytes e recusa o que passar disso, e um
 * caractere acentuado ocupa 2 bytes. {@code null} é válido.
 */
@Documented
@Constraint(validatedBy = MaxBytesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxBytes {

    int value();

    String message() default "Texto muito longo";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
