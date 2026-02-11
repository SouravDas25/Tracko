package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.User;
import com.trako.models.request.AuthicationRequest;
import com.trako.models.request.LoginRequest;
import com.trako.models.request.UserSaveRequest;
import com.trako.repositories.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class SessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    @MockBean
    private AuthenticationManager authenticationManager;

    private User testUser;

    @BeforeEach
    public void setup() {
        usersRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        testUser.setFireBaseId("password");
        testUser = usersRepository.save(testUser);

        // For /api/oauth/token we mock the authentication manager so we don't depend on password encoding config.
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(),
                testUser.getFireBaseId(),
                Collections.emptyList()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
    }

    @Test
    public void loginSuccessReturnsJwtToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(testUser.getPhoneNo());
        req.setPassword(testUser.getFireBaseId());

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    public void loginWrongPasswordReturnsUnauthorized() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(testUser.getPhoneNo());
        req.setPassword("wrong");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void oauthTokenReturnsJwtToken() throws Exception {
        AuthicationRequest req = new AuthicationRequest();
        req.setPhoneNo(testUser.getPhoneNo());
        req.setFirebaseUuid(testUser.getFireBaseId());

        mockMvc.perform(post("/api/oauth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    public void signUpWithoutUuidReturnsUnauthorized() throws Exception {
        UserSaveRequest req = new UserSaveRequest();
        req.setName("NoUuid");
        req.setPhoneNo("9998887777");
        // uuid missing

        mockMvc.perform(post("/api/signUp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
