package com.trako.configs;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.Set;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT token. Obtain via POST /api/oauth/token or POST /api/login"
)
public class OpenApiConfig {

    private static final Set<String> EXCLUDED_METHODS = Set.of("signIn", "login", "health");

    @Bean
    public OpenAPI trackoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tracko API")
                        .version("1.0")
                        .description("Expense management REST API"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OperationCustomizer apiResponseWrappingCustomizer() {
        return (operation, handlerMethod) -> {
            if (shouldSkip(handlerMethod)) {
                return operation;
            }

            ApiResponse ok = operation.getResponses().get("200");
            if (ok == null || ok.getContent() == null) {
                return operation;
            }

            MediaType json = ok.getContent().get("application/json");
            if (json == null || json.getSchema() == null) {
                // Try wildcard content type
                json = ok.getContent().get("*/*");
                if (json == null || json.getSchema() == null) {
                    return operation;
                }
            }

            Schema<?> originalSchema = json.getSchema();

            // Build the ApiResponse envelope schema
            Schema<?> wrapper = new ObjectSchema()
                    .addProperty("result", originalSchema)
                    .addProperty("message", new StringSchema()
                            .description("Human-readable status message")
                            .example("Resource retrieved successfully"));

            // Replace the response content with the wrapped schema
            ok.setContent(new Content().addMediaType("application/json",
                    new MediaType().schema(wrapper)));

            return operation;
        };
    }

    private boolean shouldSkip(HandlerMethod handlerMethod) {
        return EXCLUDED_METHODS.contains(handlerMethod.getMethod().getName());
    }
}
