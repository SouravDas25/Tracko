package com.trako.models.responses;

import com.trako.entities.ChatGroup;
import com.trako.entities.User;

import java.util.List;

public class GetGroupsResponse {

    ChatGroup chatGroup;
    List<User> userList;

    public ChatGroup getChatGroup() {
        return chatGroup;
    }

    public void setChatGroup(ChatGroup chatGroup) {
        this.chatGroup = chatGroup;
    }

    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
    }
}
