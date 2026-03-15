package com.trako.integration.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.trako.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bug Condition Exploration Test — Property 1: ApiResponse Wrapper Schema Accuracy
 *
 * This test encodes the EXPECTED (correct) behavior: every endpoint that wraps its
 * payload in {@code ApiResponse<T>} at runtime must have an OpenAPI schema describing
 * an object with {@code result} and {@code message} properties.
 *
 * On UNFIXED code this test MUST FAIL, proving the bug exists (schemas describe bare
 * inner types instead of the ApiResponse envelope).
 *
 * After the fix is applied, the same test should PASS.
 */
@SpringBootTest
@ActiveProfiles("test")
public class OpenApiBugConditionTest extends BaseIntegrationTest {

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
            // e.g. "#/components/schemas/User" -> components.schemas.User
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

    /**
     * Asserts that the given schema describes an ApiResponse envelope:
     * an object with "result" and "message" properties where "message" is a string.
     */
    private void assertApiResponseEnvelope(JsonNode schema, String endpoint) {
        JsonNode resolved = resolveSchema(schema);

        // Must be an object type
        String type = resolved.path("type").asText("");
        assertEquals("object", type,
                endpoint + ": expected schema type 'object' for ApiResponse envelope, but got '" + type + "'");

        // Must have "properties" containing "result" and "message"
        JsonNode properties = resolved.path("properties");
        assertFalse(properties.isMissingNode(),
                endpoint + ": schema has no 'properties' — not an ApiResponse envelope");

        assertTrue(properties.has("result"),
                endpoint + ": schema missing 'result' property — not an ApiResponse envelope. " +
                "Actual properties: " + properties.fieldNames().toString());

        assertTrue(properties.has("message"),
                endpoint + ": schema missing 'message' property — not an ApiResponse envelope. " +
                "Actual properties: " + properties.fieldNames().toString());

        // "message" must be typed as string
        JsonNode messageSchema = resolveSchema(properties.path("message"));
        assertEquals("string", messageSchema.path("type").asText(""),
                endpoint + ": 'message' property should be type 'string'");
    }

    @Test
    public void getUserMe_schemaShouldDescribeApiResponseWrapper() {
        JsonNode schema = getResponseSchema("/api/user/me", "get");
        assertApiResponseEnvelope(schema, "GET /api/user/me");

        // Additionally verify "result" references User-like schema
        JsonNode resultSchema = resolveSchema(
                resolveSchema(schema).path("properties").path("result"));
        assertTrue(resultSchema.has("properties"),
                "GET /api/user/me: 'result' should reference a User object schema");
    }

    @Test
    public void getCategories_schemaShouldDescribeApiResponseWrapper() {
        JsonNode schema = getResponseSchema("/api/categories", "get");
        assertApiResponseEnvelope(schema, "GET /api/categories");

        // "result" should be an array of Category
        JsonNode resultSchema = resolveSchema(
                resolveSchema(schema).path("properties").path("result"));
        assertEquals("array", resultSchema.path("type").asText(""),
                "GET /api/categories: 'result' should be an array type");
    }

    @Test
    public void getBudget_schemaShouldDescribeApiResponseWrapper() {
        JsonNode schema = getResponseSchema("/api/budget", "get");
        assertApiResponseEnvelope(schema, "GET /api/budget");

        // "result" should reference BudgetResponseDTO-like schema
        JsonNode resultSchema = resolveSchema(
                resolveSchema(schema).path("properties").path("result"));
        assertTrue(resultSchema.has("properties"),
                "GET /api/budget: 'result' should reference a BudgetResponseDTO object schema");
    }

    @Test
    public void getTotalIncome_schemaShouldDescribeApiResponseWrapper() {
        JsonNode schema = getResponseSchema("/api/transactions/total-income", "get");
        assertApiResponseEnvelope(schema, "GET /api/transactions/total-income");

        // "result" should be a number type
        JsonNode resultSchema = resolveSchema(
                resolveSchema(schema).path("properties").path("result"));
        String resultType = resultSchema.path("type").asText("");
        assertTrue(resultType.equals("number") || resultType.equals("integer"),
                "GET /api/transactions/total-income: 'result' should be a numeric type, got '" + resultType + "'");
    }
}
