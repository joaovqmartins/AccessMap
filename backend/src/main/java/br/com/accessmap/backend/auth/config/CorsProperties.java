package br.com.accessmap.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origens liberadas para CORS. Só serve para rodar o app no navegador em desenvolvimento
 * ({@code expo start --web}); app mobile não passa por CORS. Vazio (o padrão) desliga o CORS.
 */
@ConfigurationProperties(prefix = "accessmap.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of()
                : allowedOrigins.stream().map(String::trim).filter(o -> !o.isEmpty()).toList();
    }
}
