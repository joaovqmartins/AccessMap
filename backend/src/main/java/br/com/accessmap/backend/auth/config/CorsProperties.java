package br.com.accessmap.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "accessmap.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
