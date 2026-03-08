package com.trako.configs;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT token. Obtain via POST /api/oauth/token or POST /api/login"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI trackoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tracko API")
                        .version("1.0")
                        .description("Expense management REST API"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
