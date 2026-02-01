package com.trako.controllers;


import com.trako.entities.ChatMessage;
import com.trako.exceptions.NotFoundException;
import com.trako.models.request.CreateChatGroupRequestObject;
import com.trako.models.request.RetriveMessageRequest;
import com.trako.models.request.SendMessageRequest;
import com.trako.models.responses.GetGroupsResponse;
import com.trako.services.ChatService;
import com.trako.services.UserService;
import com.trako.util.Response;
import com.trako.exceptions.UserNotLoggedInException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatAPIController {

    private static final Logger log = LoggerFactory.getLogger(ChatAPIController.class);

    @Autowired
    ChatService chatService;

    @Autowired
    UserService userService;

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    ResponseEntity<?> create(@RequestBody CreateChatGroupRequestObject requestObject) {
        log.info("Chat group save request : {} ", requestObject);
        try {
            String group = chatService.createGroup(requestObject.getName(), requestObject.getUsers());
            log.info("Chat group saved : {} ", group);
            return Response.ok(group, "Chat Group created successfully.");
        } catch (NotFoundException e) {
            log.error("Chat group save failed : ", e);
            return Response.notFound();
        }
    }

    @RequestMapping(value = "/send", method = RequestMethod.POST)
    ResponseEntity<?> sendMesssage(@RequestBody SendMessageRequest sendMessageRequest) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            chatService.sendMessage(currentUserId, sendMessageRequest.getChatGroupAddress(),
                    sendMessageRequest.getMessage());
            return Response.ok("SUCCESS");
        } catch (NotFoundException e) {
            return Response.notFound();
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }

    }


    @RequestMapping(value = "/messages", method = {RequestMethod.GET, RequestMethod.POST})
    ResponseEntity<?> sendMesssage(@RequestBody RetriveMessageRequest retriveMessageRequest) {
        List<ChatMessage> chatMessages = chatService.getChatMessages(retriveMessageRequest);
        return Response.ok(chatMessages);
    }

    @RequestMapping(value = "/groups/{id}", method = RequestMethod.GET)
    ResponseEntity<?> getGroups(@PathVariable("id") String userId) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            if (!currentUserId.equals(userId)) {
                return Response.unauthorized();
            }
            List<GetGroupsResponse> groupsByUser = chatService.getGroupsByUser(currentUserId);
            return Response.ok(groupsByUser);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

}
