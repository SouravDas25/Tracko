package com.expense.manager.controllers;


import com.expense.manager.entities.ChatMessage;
import com.expense.manager.models.request.CreateChatGroupRequestObject;
import com.expense.manager.models.request.RetriveMessageRequest;
import com.expense.manager.models.request.SendMessageRequest;
import com.expense.manager.services.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatAPIController {

    private static final Logger log = LoggerFactory.getLogger(ChatAPIController.class);

    @Autowired
    ChatService chatService;


    @RequestMapping(value = "/create", method = RequestMethod.POST)
    Integer create(@RequestBody CreateChatGroupRequestObject requestObject) {
        Integer groupId = chatService.createGroup(requestObject.getName(), requestObject.getUsers());
        return groupId;
    }

    @RequestMapping(value = "/send", method = RequestMethod.POST)
    String sendMesssage(@RequestBody SendMessageRequest sendMessageRequest) {
        chatService.sendMessage(sendMessageRequest.getFrom(), sendMessageRequest.getTo(), sendMessageRequest.getMessage());
        return "SUCCESS";
    }


    @RequestMapping(value = "/getMessage", method = RequestMethod.GET)
    List<ChatMessage> sendMesssage(@RequestBody RetriveMessageRequest retriveMessageRequest) {
        return chatService.getChatMessages(retriveMessageRequest);
    }

}
