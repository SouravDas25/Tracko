package com.trako.entities;

public class ChatMessage extends AbstractBaseEntity {

    private String groupId;

    private String userId;

    private String message;

    private Integer isRead;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getIsRead() {
        return isRead;
    }

    public void setIsRead(Integer read) {
        this.isRead = read;
    }

    public String toString() {
        return "ChatMessage{id=" + id +
                ", groupId=" + groupId +
                ", userId=" + userId +
                ", message=" + message +
                ", read=" + isRead +
                "}";
    }
}