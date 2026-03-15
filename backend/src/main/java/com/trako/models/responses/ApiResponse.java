package com.trako.models.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard API response envelope")
public class ApiResponse<T> {
    @Schema(description = "Response payload")
    private T result;

    @Schema(description = "Human-readable status message", example = "Resource retrieved successfully")
    private String message;

    public static <T> ApiResponse<T> make(T object, String message) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.message = message;
        apiResponse.result = object;
        return apiResponse;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
