package com.trako.controllers;

import com.trako.models.request.AuthicationRequest;
import com.trako.models.request.LoginRequest;
import com.trako.models.request.UserSaveRequest;
import com.trako.models.responses.JwtResponse;
import com.trako.services.UserService;
import com.trako.util.JwtTokenUtil;
import com.trako.util.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    private static final Logger log = LoggerFactory.getLogger(SessionController.class);

    @Autowired
    UserService userService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/api/oauth/token")
    public ResponseEntity<?> signIn(@RequestBody AuthicationRequest authicationRequest) {

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(authicationRequest.getPhoneNo(), authicationRequest.getFirebaseUuid());
        Authentication authenticate = authenticationManager.authenticate(token);

        String jwtToken = jwtTokenUtil.generateToken((UserDetails) authenticate.getPrincipal());

        return ResponseEntity.ok(new JwtResponse(jwtToken));
    }

    @PostMapping("/api/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            UserDetails user = userDetailsService.loadUserByUsername(loginRequest.getUsername());

            if (user == null) {
                return Response.unauthorized();
            }

            boolean ok = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
            if (!ok && user != null) {
                ok = loginRequest.getPassword() != null && loginRequest.getPassword().equals(user.getPassword());
            }
            if (!ok) {
                log.warn("Login failed for username={}", loginRequest.getUsername());
                return Response.unauthorized();
            }
            String jwtToken = jwtTokenUtil.generateToken(user);
            return ResponseEntity.ok(new JwtResponse(jwtToken));
        } catch (AuthenticationException ex) {
            log.warn("AuthenticationException during login for username={}", loginRequest.getUsername());
            return Response.unauthorized();
        }
    }

    @PostMapping(value = "/api/signUp")
    ResponseEntity<?> signUp(@RequestBody UserSaveRequest userSaveRequest) {
        if (userSaveRequest.getFireBaseId() == null || userSaveRequest.getFireBaseId().isEmpty())
            return Response.unauthorized();
        String id = userService.save(userSaveRequest);
        if (id == null)
            Response.badRequest("Phone Number Incorrect");
        log.info("User Saved : {}", id);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userSaveRequest.getPhoneNo());
        String jwtToken = jwtTokenUtil.generateToken(userDetails);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Jwt-Token", jwtToken);
        return Response.ok(id, "User Saved Successfully.", responseHeaders);
    }


}
