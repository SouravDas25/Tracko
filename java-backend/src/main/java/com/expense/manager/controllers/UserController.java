package com.expense.manager.controllers;

import com.expense.manager.entities.User;
import com.expense.manager.repositories.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;


@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UsersRepository usersRepository;

    @RequestMapping(value = {"", "/{id}"})
    List<User> show(@PathVariable(value = "id", required = false) Integer id) {
        if(id == null){
            return usersRepository.findAll();
        }
        return Collections.singletonList(usersRepository.findOne(id));
    }

    @RequestMapping(value = "/save")
    ResponseEntity<String> save(@RequestBody User user){
        usersRepository.save(user);
        return ResponseEntity.ok(user.toString());
    }


}
