package com.trako.services;

import com.trako.entities.User;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.models.request.UserSaveRequest;
import com.trako.repositories.UsersRepository;
import com.trako.util.CommonUtil;
import com.trako.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    public User loggedInUser() throws UserNotLoggedInException {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String phoneNo = null;
        if (principal instanceof UserDetails) {
            phoneNo = ((UserDetails) principal).getUsername();
            return findByPhoneNo(phoneNo);
        } else {
            throw new UserNotLoggedInException();
        }

    }

    public List<User> findUser(String id) {
        if (id == null) {
            return usersRepository.findAll();
        }
        return Collections.singletonList(usersRepository.findById(id).orElse(null));
    }

    public User findByPhoneNo(String phoneNo) {
        phoneNo = CommonUtil.extractPhoneNumber(phoneNo);
        return usersRepository.findByPhoneNo(phoneNo);
    }

    public User findById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return usersRepository.findById(id).orElse(null);
    }

    public String save(UserSaveRequest userSaveRequest) {

        log.info("User Save Request : {}", userSaveRequest);
        String phnNo = CommonUtil.extractPhoneNumber(userSaveRequest.getPhoneNo());
        if (phnNo == null) {
            return null;
        }

        User user;
        User isPreset = usersRepository.findByPhoneNo(phnNo);
        if (isPreset != null) {
            user = CommonUtil.mapModel(isPreset, User.class);
        } else {
            user = new User();
        }
        if (!userSaveRequest.isShadow() || isPreset == null) {
            CommonUtil.mapModel(userSaveRequest, user);
        }

        user.setPhoneNo(phnNo);


        if (isPreset != null) {
            user.setId(isPreset.getId());
        } else {
            user.setId(null);
        }
        if (user.isShadow() && isPreset != null) {
            if (isPreset.getEmail() != null && user.getEmail() == null)
                user.setEmail(isPreset.getEmail());
            if (isPreset.getName() != null && user.getName() == null)
                user.setName(isPreset.getName());
        }
        
        if (userSaveRequest.getBaseCurrency() != null) {
            user.setBaseCurrency(userSaveRequest.getBaseCurrency());
        } else if (user.getBaseCurrency() == null) {
            user.setBaseCurrency("INR");
        }
        
        user = usersRepository.save(user);
        return user.getId();
    }

    public User saveUser(User user) {
        return usersRepository.save(user);
    }
}
