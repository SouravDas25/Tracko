package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "chat_messages")
public class ChatMessage extends AbstractBaseEntity {

    @Column(name = "group_id")
    private String groupId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "message")
    private String message;

    @JsonFormat(pattern = "dd-MM-yyyy")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Date createdAt;

    @Column(name = "is_read")
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

    public Date getCreatedAt() {
        return createdAt;
    }

//  public void setCreatedAt(Date createdAt) {
//    this.createdAt = createdAt;
//  }

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
                ", createdAt=" + createdAt +
                ", read=" + isRead +
                "}";
    }
}