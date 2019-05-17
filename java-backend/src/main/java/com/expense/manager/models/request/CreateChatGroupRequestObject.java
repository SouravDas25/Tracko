package com.expense.manager.models.request;

import com.expense.manager.entities.Group;
import com.expense.manager.entities.UserGroup;

import java.util.ArrayList;
import java.util.List;

public class CreateChatGroupRequestObject {


    String name;
    List<String> users;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }
}
