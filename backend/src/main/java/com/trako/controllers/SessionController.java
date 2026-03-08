package com.trako.controllers;

import com.trako.models.request.AuthicationRequest;
import com.trako.models.request.LoginRequest;
import com.trako.models.responses.JwtResponse;
import com.trako.services.UserService;
import com.trako.util.JwtTokenUtil;
import com.trako.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

@Tag(name = "Authentication", description = "Obtain a JWT token")
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

    @Operation(summary = "Sign in with phone number and password")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = JwtResponse.class)))
    @SecurityRequirements
    @PostMapping("/api/oauth/token")
    public ResponseEntity<?> signIn(@Valid @RequestBody AuthicationRequest authicationRequest) {

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(authicationRequest.getPhoneNo(), authicationRequest.getPassword());
        Authentication authenticate = authenticationManager.authenticate(token);

        String jwtToken = jwtTokenUtil.generateToken((UserDetails) authenticate.getPrincipal());

        return ResponseEntity.ok(new JwtResponse(jwtToken));
    }

    @Operation(summary = "Log in with username and password")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = JwtResponse.class)))
    @SecurityRequirements
    @PostMapping("/api/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
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


}
