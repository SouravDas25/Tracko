package com.trako.models.request;

public class RetriveMessageRequest {

    private String chatGroup;
    private String retrieveFrom;

    public String getChatGroup() {
        return chatGroup;
    }

    public void setChatGroup(String chatGroup) {
        this.chatGroup = chatGroup;
    }

    public String getRetrieveFrom() {
        return retrieveFrom;
    }

    public void setRetrieveFrom(String retrieveFrom) {
        this.retrieveFrom = retrieveFrom;
    }
}
