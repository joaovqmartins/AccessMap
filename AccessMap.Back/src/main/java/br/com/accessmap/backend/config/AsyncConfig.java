package br.com.accessmap.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Liga o {@code @Async}: o envio de e-mail não segura a resposta da API. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
