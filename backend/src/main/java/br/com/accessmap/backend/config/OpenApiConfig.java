package br.com.accessmap.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI accessMapOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token obtido em /api/auth/login ou /api/auth/register")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .info(new Info()
                        .title("AccessMap API")
                        .description("API da plataforma colaborativa de avaliação de acessibilidade de "
                                + "estabelecimentos: cadastro de usuários, cache de locais (Google Place ID) "
                                + "e avaliações de acessibilidade com sistema de confiabilidade.")
                        .version("v0.1")
                        .contact(new Contact().name("Equipe AccessMap")));
    }
}
