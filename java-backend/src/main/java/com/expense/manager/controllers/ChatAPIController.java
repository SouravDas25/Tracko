package com.expense.manager.controllers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatAPIController {

    private static final Logger log = LoggerFactory.getLogger(ChatAPIController.class);


    @RequestMapping(value="/getImage")
    ResponseEntity<Void> getImageUrl(@RequestParam(name = "domain") String domain) {
        return ResponseEntity.ok().build();
    }


}
