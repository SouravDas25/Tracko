package com.trako.entities;

public class UserChatGroup extends AbstractBaseEntity {

    private String userId;

    private String groupId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String toString() {
        return "UserChatGroup{userId=" + userId +
                ", groupId=" + groupId +
                ", id=" + id +
                "}";
    }
}