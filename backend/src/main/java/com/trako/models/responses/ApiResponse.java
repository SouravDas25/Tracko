package com.trako.models.responses;

public class ApiResponse {
    private Object result;
    private String message;

    public static ApiResponse make(Object object, String message) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.message = message;
        apiResponse.result = object;
        return apiResponse;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
