package com.expense.manager.services;


import com.expense.manager.entities.User;
import com.expense.manager.repositories.GroupsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatService {

    @Autowired
    GroupsRepository groupsRepository;

    public Integer createGroup(List<User> users) {
        return 0;
    }

    public boolean sendMessage(User from, User to, String message) {
        return true;
    }


}
