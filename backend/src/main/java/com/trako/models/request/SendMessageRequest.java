package com.trako.models.request;

public class SendMessageRequest {

    private String sender;
    private String chatGroupAddress;
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getChatGroupAddress() {
        return chatGroupAddress;
    }

    public void setChatGroupAddress(String chatGroupAddress) {
        this.chatGroupAddress = chatGroupAddress;
    }
}
