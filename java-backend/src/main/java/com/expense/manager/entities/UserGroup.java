package com.expense.manager.entities;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "user_groups")
@Entity
public class UserGroup implements Serializable {
  private static final long serialVersionUID = 1L;

  @Column(name = "user_id")
  private Integer userId;

  @Column(name = "group_id")
  private Integer groupId;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", insertable = false, nullable = false)
  private Integer id;

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public Integer getGroupId() {
    return groupId;
  }

  public void setGroupId(Integer groupId) {
    this.groupId = groupId;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String toString() {
    return "UserGroup{userId=" + userId +
      ", groupId=" + groupId + 
      ", id=" + id + 
      "}";
  }
}