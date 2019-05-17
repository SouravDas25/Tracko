package com.expense.manager.entities;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import javax.persistence.*;

@Entity
@Table(name = "chat_messages")
public class ChatMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", insertable = false, nullable = false)
  private Integer id;

  @Column(name = "group_id")
  private Integer groupId;

  @Column(name = "user_id")
  private Integer userId;

  @Column(name = "message")
  private String message;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
  private Date createdAt;

  @Column(name = "is_read")
  private Integer isRead;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getGroupId() {
    return groupId;
  }

  public void setGroupId(Integer groupId) {
    this.groupId = groupId;
  }

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
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