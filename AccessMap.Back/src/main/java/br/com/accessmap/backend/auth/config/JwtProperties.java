package br.com.accessmap.backend.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "accessmap.jwt")
public record JwtProperties(

        @NotBlank(message = "Defina a variavel de ambiente JWT_SECRET (veja .env.example)")
        @Size(min = 32, message = "Defina a variavel de ambiente JWT_SECRET com no minimo 32 caracteres (veja .env.example)")
        String secret,

        @NotNull Duration accessTokenTtl,

        @NotNull Duration refreshTokenTtl,

        @NotBlank String issuer
) {
}
