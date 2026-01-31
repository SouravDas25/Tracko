package com.trako.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import com.trako.repositories.UsersRepository;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    @Autowired
    UsersRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String phoneNo) throws UsernameNotFoundException {
        com.trako.entities.User byPhoneNo = usersRepository.findByPhoneNo(phoneNo);
        if (byPhoneNo == null) {
            throw new UsernameNotFoundException("User not found in DB.");
        }
        return new User(byPhoneNo.getPhoneNo(), byPhoneNo.getFirebase_uuid(), new ArrayList<>());
    }
}