package com.trako.services;


import com.trako.controllers.ChatAPIController;
import com.trako.entities.ChatGroup;
import com.trako.entities.ChatMessage;
import com.trako.entities.User;
import com.trako.entities.UserChatGroup;
import com.trako.exceptions.NotFoundException;
import com.trako.models.request.RetriveMessageRequest;
import com.trako.models.responses.GetGroupsResponse;
import com.trako.repositories.ChatMessagesRepository;
import com.trako.repositories.GroupsRepository;
import com.trako.repositories.UserGroupsRepository;
import com.trako.repositories.UsersRepository;
import com.trako.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.ArrayList;
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

    @Transactional
    public String createGroup(String groupName, List<String> phoneNumbers) throws NotFoundException {
        if (phoneNumbers.size() == 2) {
            String groupId = getCommonGroupId(phoneNumbers.get(0), phoneNumbers.get(1));
            if (groupId != null)
                return groupId;
        }
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(groupName);
        groupsRepository.save(chatGroup);
        Set<String> phoneNumberSet = new HashSet<>(phoneNumbers);
        for (String phoneNumber : phoneNumberSet) {
            phoneNumber = CommonUtil.extractPhoneNumber(phoneNumber);
            User user = usersRepository.findByPhoneNo(phoneNumber);
            if (user == null) {
                throw new NotFoundException();
            }
            UserChatGroup userChatGroup = new UserChatGroup();
            userChatGroup.setUserId(user.getId());
            userChatGroup.setGroupId(chatGroup.getId());
            userGroupsRepository.save(userChatGroup);
        }
        return chatGroup.getId();
    }

    private String getCommonGroupId(String phoneNumber1, String phoneNumber2) throws NotFoundException {
        phoneNumber1 = CommonUtil.extractPhoneNumber(phoneNumber1);
        phoneNumber2 = CommonUtil.extractPhoneNumber(phoneNumber2);
        User user1 = usersRepository.findByPhoneNo(phoneNumber1);
        User user2 = usersRepository.findByPhoneNo(phoneNumber2);
        if (user1 == null || user2 == null) {
            throw new NotFoundException();
        }
        List<UserChatGroup> userChatGroups1 = userGroupsRepository.findByUserId(user1.getId());
        List<UserChatGroup> userChatGroups2 = userGroupsRepository.findByUserId(user2.getId());
        Set<String> groupIds1 = new HashSet<>();
        for (UserChatGroup userChatGroup : userChatGroups1)
            groupIds1.add(userChatGroup.getGroupId());

        Set<String> groupIds2 = new HashSet<>();
        for (UserChatGroup userChatGroup : userChatGroups2)
            groupIds2.add(userChatGroup.getGroupId());
        groupIds1.retainAll(groupIds2);
        if (groupIds1.size() > 1) {
            return groupIds1.iterator().next();
        }
        return null;

    }

    public boolean sendMessage(String userId, String groupId, String message) throws NotFoundException {
        User user = usersRepository.findOne(userId);
        ChatGroup chatGroup = groupsRepository.findOne(groupId);
        if (user == null || chatGroup == null) {
            throw new NotFoundException();
        }
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setGroupId(chatGroup.getId());
        chatMessage.setUserId(user.getId());
        chatMessage.setMessage(message.trim());
        chatMessage.setIsRead(0);
        chatMessagesRepository.save(chatMessage);
        return chatMessage.getId() != null;
    }


    public List<ChatMessage> getChatMessages(RetriveMessageRequest retriveMessageRequest) {
        log.info("GROUP ID : {}", retriveMessageRequest.getChatGroup());
        log.info("FROM : {}", retriveMessageRequest.getRetrieveFrom());
        if (retriveMessageRequest.getRetrieveFrom() == null) {
            return chatMessagesRepository.retrieveMessagesLatest(retriveMessageRequest.getChatGroup());
        }
        return chatMessagesRepository.retrieveMessages(retriveMessageRequest.getChatGroup(),
                retriveMessageRequest.getRetrieveFrom());
    }

    public List<GetGroupsResponse> getGroupsByUser(String userId) {
        List<UserChatGroup> userChatGroups = userGroupsRepository.findByUserId(userId);
        List<GetGroupsResponse> groupsResponses = new ArrayList<>();
        for (UserChatGroup userChatGroup : userChatGroups) {
            GetGroupsResponse getGroupsResponse = new GetGroupsResponse();
            getGroupsResponse.setChatGroup(userChatGroup.getChatGroup());
            List<User> inRoom = new ArrayList<>();
            for (UserChatGroup userg : userGroupsRepository.findByGroupId(userChatGroup.getGroupId())) {
                inRoom.add(userg.getUser());
            }
            getGroupsResponse.setUserList(inRoom);
            groupsResponses.add(getGroupsResponse);
        }
        return groupsResponses;
    }

}
