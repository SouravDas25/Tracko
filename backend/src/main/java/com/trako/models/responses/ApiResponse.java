package com.trako.models.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Standard API response envelope")
@Getter
@Setter
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
}
