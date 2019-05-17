package com.expense.manager.models.request;

public class RetriveMessageRequest {

    Integer groupId;
    private Integer retrieveFrom;

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getRetrieveFrom() {
        return retrieveFrom;
    }

    public void setRetrieveFrom(Integer retrieveFrom) {
        this.retrieveFrom = retrieveFrom;
    }

}
