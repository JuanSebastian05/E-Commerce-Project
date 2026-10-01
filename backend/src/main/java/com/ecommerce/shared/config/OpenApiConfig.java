package com.ecommerce.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    /** Incluye el esquema Bearer para poder probar los endpoints protegidos desde Swagger UI. */
    @Bean
    OpenAPI ecommerceOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("E-Commerce API")
                .version("v1")
                .description("API REST del e-commerce de productos tecnológicos"))
            .components(new Components().addSecuritySchemes(BEARER,
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
