package br.com.accessmap.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI accessMapOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AccessMap API")
                        .description("API da plataforma colaborativa de avaliação de acessibilidade de "
                                + "estabelecimentos: cadastro de usuários, cache de locais (Google Place ID) "
                                + "e avaliações de acessibilidade com sistema de confiabilidade.")
                        .version("v0.1")
                        .contact(new Contact().name("Equipe AccessMap")));
    }
}
