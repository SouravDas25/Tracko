package com.trako.controllers;

import com.trako.entities.User;
import com.trako.models.request.UserSaveRequest;
import com.trako.services.UserService;
import com.trako.util.Response;
import org.hibernate.validator.constraints.SafeHtml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;


@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;

    @GetMapping(value = {"", "/{id}"})
    ResponseEntity<?> show(@PathVariable(required = false) String id) {
        List<User> users = userService.findUser(id);
        return Response.ok(users);
    }

    @GetMapping(value = "/byPhoneNo")
    ResponseEntity<?> showByPhone(@RequestParam("phone_no") String phoneNo) {
        User byPhoneNo = userService.findByPhoneNo(phoneNo);
        if (byPhoneNo == null)
            return Response.ok("Resource Empty");
        List<User> users = Collections.singletonList(byPhoneNo);
        return Response.ok(users);
    }

    @PostMapping(value = "/save")
    ResponseEntity<?> save(@SafeHtml @RequestBody UserSaveRequest userSaveRequest) {
        String id = userService.save(userSaveRequest);
        if (id == null)
            Response.badRequest("Phone Number Incorrect");
        log.info("User Saved : {}", id);
        return Response.ok(id, "User Saved Successfully.");
    }

}
