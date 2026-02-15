package com.trako.util;

import com.trako.models.responses.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


public class Response {

    public static ResponseEntity<?> ok(String message) {
        return ResponseEntity.ok(ApiResponse.make(null, message));
    }

    public static ResponseEntity<?> ok(Object result) {
        return ResponseEntity.ok(ApiResponse.make(result, "Resource retrieved successfully"));
    }

    public static ResponseEntity<?> ok(Object result, String message) {
        return ResponseEntity.ok(ApiResponse.make(result, message));
    }

    public static ResponseEntity<?> ok(Object result, String message, HttpHeaders headers) {
        return ResponseEntity.ok().headers(headers).body(ApiResponse.make(result, message));
    }

    public static ResponseEntity<?> notFound() {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.make(null, "Resource not found."));
    }

    public static ResponseEntity<?> notFound(String message) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.make(null, message));
    }

    public static ResponseEntity<?> unauthorized() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.make(null, "Unauthorized Request."));
    }

    public static ResponseEntity<?> badRequest(String message) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.make(null, message));
    }


}
