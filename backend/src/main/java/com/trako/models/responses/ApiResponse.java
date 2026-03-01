package com.trako.models.responses;

public class ApiResponse<T> {
    private T result;
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
