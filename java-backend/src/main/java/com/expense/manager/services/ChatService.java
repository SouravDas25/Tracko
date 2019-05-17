package com.expense.manager.services;


import com.expense.manager.controllers.ChatAPIController;
import com.expense.manager.entities.ChatMessage;
import com.expense.manager.entities.Group;
import com.expense.manager.entities.User;
import com.expense.manager.entities.UserGroup;
import com.expense.manager.models.request.RetriveMessageRequest;
import com.expense.manager.repositories.ChatMessagesRepository;
import com.expense.manager.repositories.GroupsRepository;
import com.expense.manager.repositories.UserGroupsRepository;
import com.expense.manager.repositories.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatAPIController.class);

    @Autowired
    GroupsRepository groupsRepository;

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    UserGroupsRepository userGroupsRepository;

    @Autowired
    ChatMessagesRepository chatMessagesRepository;

    public Integer createGroup(String groupName, List<String> phoneNumbers) {
        Group group = new Group();
        group.setName(groupName);
        groupsRepository.save(group);
        Set<String> phoneNumberSet = new HashSet<>(phoneNumbers);
        for (String phoneNumber : phoneNumberSet) {
            User user = usersRepository.findByPhoneNo(phoneNumber);
            UserGroup userGroup = new UserGroup();
            userGroup.setUserId(user.getId());
            userGroup.setGroupId(group.getId());
            userGroupsRepository.save(userGroup);
        }
        return group.getId();
    }

    public boolean sendMessage(Integer userId, Integer groupId, String message) {
        User user = usersRepository.findOne(userId);
        Group group = groupsRepository.findOne(groupId);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setGroupId(group.getId());
        chatMessage.setUserId(user.getId());
        chatMessage.setMessage(message.trim());
        chatMessage.setIsRead(0);
        chatMessagesRepository.save(chatMessage);
        return chatMessage.getId() != null;
    }


    public List<ChatMessage> getChatMessages(RetriveMessageRequest retriveMessageRequest) {
        log.info("GROUP ID : {}",retriveMessageRequest.getGroupId().toString());
        log.info("FROM : {}" , retriveMessageRequest.getRetrieveFrom());
        if (retriveMessageRequest.getRetrieveFrom() == null) {
            return chatMessagesRepository.retrieveMessagesLatest(retriveMessageRequest.getGroupId());
        }
        return chatMessagesRepository.retrieveMessages(retriveMessageRequest.getGroupId(),
                retriveMessageRequest.getRetrieveFrom());
    }

}
