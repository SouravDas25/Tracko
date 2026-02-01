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
        com.trako.entities.User u = usersRepository.findByPhoneNo(phoneNo);
        if (u == null) {
            u = usersRepository.findByEmail(phoneNo);
        }
        if (u == null) {
            u = usersRepository.findByName(phoneNo);
        }
        if (u == null) {
            throw new UsernameNotFoundException("User not found in DB.");
        }
        return new User(u.getPhoneNo(), u.getFireBaseId(), new ArrayList<>());
    }
}