package com.expense.manager.models.request;

public class SendMessageRequest {

    Integer from;
    Integer to;
    String message;

    public String getMessage() {
        return message;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getTo() {
        return to;
    }

    public void setTo(Integer to) {
        this.to = to;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
