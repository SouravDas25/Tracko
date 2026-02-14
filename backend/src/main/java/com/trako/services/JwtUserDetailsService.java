package com.trako.services;

import com.trako.repositories.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (u.isAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return new User(u.getPhoneNo(), u.getFireBaseId(), authorities);
    }
}