package com.trako.integration.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.trako.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Preservation Property Test — Property 2: Non-Wrapped Endpoint and Runtime Behavior Integrity
 *
 * These tests observe and lock down the current (UNFIXED) behavior of endpoints that
 * do NOT use the {@code ApiResponse<T>} wrapper. They MUST PASS on unfixed code,
 * establishing a baseline that the fix must preserve.
 *
 * Covered behaviors:
 * <ul>
 *   <li>{@code POST /api/oauth/token} schema references JwtResponse (not ApiResponse)</li>
 *   <li>{@code POST /api/login} schema references JwtResponse (not ApiResponse)</li>
 *   <li>{@code GET /api/health} schema is a plain object without result/message wrapper</li>
 *   <li>Swagger UI is accessible at {@code /swagger-ui/index.html}</li>
 *   <li>All existing controller endpoints are present in the OpenAPI paths</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
public class OpenApiPreservationTest extends BaseIntegrationTest {

    private JsonNode openApiSpec;

    @BeforeEach
    public void fetchOpenApiSpec() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        openApiSpec = objectMapper.readTree(json);
    }

    /**
     * Resolves a schema node, following $ref if present.
     */
    private JsonNode resolveSchema(JsonNode schema) {
        if (schema.has("$ref")) {
            String ref = schema.get("$ref").asText();
            String[] parts = ref.replace("#/", "").split("/");
            JsonNode resolved = openApiSpec;
            for (String part : parts) {
                resolved = resolved.path(part);
            }
            return resolved;
        }
        return schema;
    }

    /**
     * Returns the 200 response schema for the given path and HTTP method.
     */
    private JsonNode getResponseSchema(String path, String method) {
        JsonNode pathItem = openApiSpec.path("paths").path(path);
        assertFalse(pathItem.isMissingNode(), "Path not found in OpenAPI spec: " + path);

        JsonNode operation = pathItem.path(method);
        assertFalse(operation.isMissingNode(), "Method " + method + " not found for path: " + path);

        JsonNode content200 = operation.path("responses").path("200").path("content");
        assertFalse(content200.isMissingNode(), "No 200 response content for " + method.toUpperCase() + " " + path);

        JsonNode mediaType = content200.path("application/json");
        if (mediaType.isMissingNode()) {
            mediaType = content200.path("*/*");
        }
        assertFalse(mediaType.isMissingNode(), "No JSON media type for " + method.toUpperCase() + " " + path);

        return mediaType.path("schema");
    }

    // ── SessionController: POST /api/oauth/token ──

    @Test
    public void oauthToken_schemaShouldReferenceJwtResponse_notApiResponse() {
        JsonNode schema = getResponseSchema("/api/oauth/token", "post");
        JsonNode resolved = resolveSchema(schema);

        // Must reference JwtResponse — should have a "token" property
        JsonNode properties = resolved.path("properties");
        assertFalse(properties.isMissingNode(),
                "POST /api/oauth/token: schema should have properties (JwtResponse)");
        assertTrue(properties.has("token"),
                "POST /api/oauth/token: schema should have 'token' property (JwtResponse)");

        // Must NOT be wrapped in ApiResponse (no "result" or "message" properties)
        assertFalse(properties.has("result"),
                "POST /api/oauth/token: schema should NOT have 'result' — must not be wrapped in ApiResponse");
        assertFalse(properties.has("message"),
                "POST /api/oauth/token: schema should NOT have 'message' — must not be wrapped in ApiResponse");
    }

    // ── SessionController: POST /api/login ──

    @Test
    public void login_schemaShouldReferenceJwtResponse_notApiResponse() {
        JsonNode schema = getResponseSchema("/api/login", "post");
        JsonNode resolved = resolveSchema(schema);

        // Must reference JwtResponse — should have a "token" property
        JsonNode properties = resolved.path("properties");
        assertFalse(properties.isMissingNode(),
                "POST /api/login: schema should have properties (JwtResponse)");
        assertTrue(properties.has("token"),
                "POST /api/login: schema should have 'token' property (JwtResponse)");

        // Must NOT be wrapped in ApiResponse
        assertFalse(properties.has("result"),
                "POST /api/login: schema should NOT have 'result' — must not be wrapped in ApiResponse");
        assertFalse(properties.has("message"),
                "POST /api/login: schema should NOT have 'message' — must not be wrapped in ApiResponse");
    }

    // ── HealthController: GET /api/health ──

    @Test
    public void health_schemaShouldNotBeWrappedInApiResponse() {
        JsonNode schema = getResponseSchema("/api/health", "get");
        JsonNode resolved = resolveSchema(schema);

        // Health endpoint returns a plain object — must NOT have result/message wrapper
        String type = resolved.path("type").asText("");
        assertEquals("object", type,
                "GET /api/health: schema should be type 'object'");

        // If properties exist, they must NOT include "result" or "message"
        JsonNode properties = resolved.path("properties");
        if (!properties.isMissingNode()) {
            assertFalse(properties.has("result"),
                    "GET /api/health: schema should NOT have 'result' — must not be wrapped in ApiResponse");
            assertFalse(properties.has("message"),
                    "GET /api/health: schema should NOT have 'message' — must not be wrapped in ApiResponse");
        }
    }

    // ── Swagger UI accessibility ──

    @Test
    public void swaggerUi_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    // ── All existing endpoints present in OpenAPI paths ──

    @Test
    public void allExpectedEndpoints_shouldBePresentInOpenApiPaths() {
        JsonNode paths = openApiSpec.path("paths");
        assertFalse(paths.isMissingNode(), "OpenAPI spec should have 'paths'");

        // Core endpoints that must be present (representative from each controller)
        List<String> expectedPaths = Arrays.asList(
                "/api/oauth/token",
                "/api/login",
                "/api/health",
                "/api/user/me",
                "/api/categories",
                "/api/budget",
                "/api/transactions/total-income"
        );

        for (String path : expectedPaths) {
            assertFalse(paths.path(path).isMissingNode(),
                    "Expected endpoint missing from OpenAPI paths: " + path);
        }
    }
}
