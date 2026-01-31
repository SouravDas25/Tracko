package com.trako.helpers;

import com.trako.entities.User;
import com.trako.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class CommonHelper {

    @Autowired
    UserService userService;

    public User loggedInUser() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String phoneNo;
            if (principal instanceof UserDetails) {
                phoneNo = ((UserDetails) principal).getUsername();
            } else {
                phoneNo = principal.toString();
            }
            return userService.findByPhoneNo(phoneNo);
        } catch (Exception e) {
            return null;
        }
    }

}
