package com.trako.controllers;

import com.trako.entities.User;
import com.trako.models.request.AuthicationRequest;
import com.trako.models.request.LoginRequest;
import com.trako.models.responses.JwtResponse;
import com.trako.repositories.UsersRepository;
import com.trako.services.LoginAttemptService;
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

    private static final String LOCKED_MESSAGE = "Too many failed login attempts. Please try again later.";

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Operation(summary = "Sign in with phone number and password")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = JwtResponse.class)))
    @SecurityRequirements
    @PostMapping("/api/oauth/token")
    public ResponseEntity<?> signIn(@Valid @RequestBody AuthicationRequest authicationRequest) {

        User user = resolveUser(authicationRequest.getPhoneNo());
        if (loginAttemptService.isLocked(user)) {
            return lockedResponse(user);
        }

        try {
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(authicationRequest.getPhoneNo(), authicationRequest.getPassword());
            Authentication authenticate = authenticationManager.authenticate(token);

            loginAttemptService.onSuccess(user);

            String jwtToken = jwtTokenUtil.generateToken((UserDetails) authenticate.getPrincipal());
            return ResponseEntity.ok(new JwtResponse(jwtToken));
        } catch (AuthenticationException ex) {
            log.warn("AuthenticationException during sign-in for phoneNo={}", authicationRequest.getPhoneNo());
            if (user != null && loginAttemptService.onFailure(user)) {
                return lockedResponse(user);
            }
            return Response.unauthorized();
        }
    }

    @Operation(summary = "Log in with username and password")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = JwtResponse.class)))
    @SecurityRequirements
    @PostMapping("/api/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            User user = resolveUser(loginRequest.getUsername());
            if (user == null) {
                return Response.unauthorized();
            }

            if (loginAttemptService.isLocked(user)) {
                return lockedResponse(user);
            }

            boolean ok = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
            if (!ok) {
                log.warn("Login failed for username={}", loginRequest.getUsername());
                if (loginAttemptService.onFailure(user)) {
                    return lockedResponse(user);
                }
                return Response.unauthorized();
            }

            loginAttemptService.onSuccess(user);

            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());
            String jwtToken = jwtTokenUtil.generateToken(userDetails);
            return ResponseEntity.ok(new JwtResponse(jwtToken));
        } catch (AuthenticationException ex) {
            log.warn("AuthenticationException during login for username={}", loginRequest.getUsername());
            return Response.unauthorized();
        }
    }

    /**
     * Resolves the persisted user by phone number then email, mirroring how
     * {@link com.trako.services.JwtUserDetailsService} resolves credentials during
     * authentication. Returns {@code null} when no matching account exists.
     */
    private User resolveUser(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        User user = usersRepository.findByPhoneNo(identifier);
        if (user == null) {
            user = usersRepository.findByEmail(identifier);
        }
        return user;
    }

    private ResponseEntity<?> lockedResponse(User user) {
        return Response.tooManyRequests(LOCKED_MESSAGE, loginAttemptService.retryAfterSeconds(user));
    }
}
