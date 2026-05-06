package com.finshield.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI finshieldOpenApi() {
        String bearerScheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("FinShield API")
                        .description("Real-time fraud detection, AML monitoring, and case investigation APIs")
                        .version("v1")
                        .contact(new Contact().name("FinShield Engineering")))
                .components(new Components().addSecuritySchemes(
                        bearerScheme,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .addSecurityItem(new SecurityRequirement().addList(bearerScheme));
    }
}
