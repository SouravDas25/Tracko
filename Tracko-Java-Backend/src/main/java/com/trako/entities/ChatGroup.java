package com.trako.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "chat_groups")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChatGroup extends AbstractBaseEntity {

    @Column(name = "name")
    private String name;

    @JsonFormat(pattern = "dd-MM-yyyy")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Date createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "chatGroup", fetch = FetchType.LAZY)
    private Set<UserChatGroup> userChatGroups;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

//  public void setCreatedAt(Date createdAt) {
//    this.createdAt = createdAt;
//  }


    public Set<UserChatGroup> getUserChatGroups() {
        return userChatGroups;
    }


    public void setUserChatGroups(Set<UserChatGroup> userChatGroups) {
        this.userChatGroups = userChatGroups;
    }

    @Override
    public String toString() {
        return "ChatGroup{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

}