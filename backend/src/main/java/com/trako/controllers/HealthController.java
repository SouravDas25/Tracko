package com.trako.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * Provides a simple endpoint to verify the backend service is running
 * This endpoint is publicly accessible and does not require authentication
 */
@Tag(name = "Health", description = "Service health check — no authentication required")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Operation(summary = "Check service health")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "object")))
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "Tracko Backend");
        return ResponseEntity.ok(response);
    }
}
