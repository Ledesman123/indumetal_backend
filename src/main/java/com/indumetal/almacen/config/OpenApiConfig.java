package com.indumetal.almacen.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI almacenOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Intranet de Gestion de Almacen - INDUMETAL PERU S.A.C.")
                        .description("API REST del Sprint 1 (Backend y Base de Datos). "
                                + "Usa POST /api/auth/login para obtener un token y pegalo "
                                + "aqui arriba en 'Authorize' como: Bearer <token>")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                                .name(ESQUEMA_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}