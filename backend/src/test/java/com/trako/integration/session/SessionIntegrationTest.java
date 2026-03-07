package com.trako.integration.session;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.User;
import com.trako.integration.BaseIntegrationTest;
import com.trako.models.request.AuthicationRequest;
import com.trako.models.request.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class SessionIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private AuthenticationManager authenticationManager;

    private User testUser;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("Test User");

        // For /api/oauth/token we mock the authentication manager so we don't depend on password encoding config.
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(),
                testUser.getPassword(),
                Collections.emptyList()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
    }

    @Test
    public void loginSuccessReturnsJwtToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(testUser.getPhoneNo());
        req.setPassword(testUser.getPassword());

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
        req.setPassword(testUser.getPassword());

        mockMvc.perform(post("/api/oauth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
