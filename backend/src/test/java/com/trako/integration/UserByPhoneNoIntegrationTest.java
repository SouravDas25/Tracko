package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.User;
import com.trako.repositories.UsersRepository;
import com.trako.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class UserByPhoneNoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private User authUser;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        usersRepository.deleteAll();

        authUser = new User();
        authUser.setName("Auth User");
        authUser.setPhoneNo("1234567890");
        authUser.setEmail("auth@example.com");
        authUser.setPassword("password");
        authUser = usersRepository.save(authUser);

        UserDetails principal = new org.springframework.security.core.userdetails.User(
                authUser.getPhoneNo(),
                authUser.getPassword(),
                Collections.emptyList()
        );
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);
    }

    @Test
    public void byPhoneNoWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/byPhoneNo")
                        .queryParam("phone_no", "1234567890"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void byPhoneNoWhenUserExistsReturnsList() throws Exception {
        User target = new User();
        target.setName("Target");
        target.setPhoneNo("9998887777");
        target.setEmail("target@example.com");
        target.setPassword("pass2");
        target = usersRepository.save(target);

        // send formatted phone to exercise CommonUtil.extractPhoneNumber
        mockMvc.perform(get("/api/user/byPhoneNo")
                        .header("Authorization", bearerToken)
                        .queryParam("phone_no", "+91-99988 87777"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].id").value(target.getId()))
                .andExpect(jsonPath("$.result[0].phoneNo").value("9998887777"));
    }

    @Test
    public void byPhoneNoWhenNotFoundReturnsResourceEmpty() throws Exception {
        mockMvc.perform(get("/api/user/byPhoneNo")
                        .header("Authorization", bearerToken)
                        .queryParam("phone_no", "0000000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Resource Empty"));
    }

    @Test
    public void byPhoneNoWithInvalidPhoneReturnsResourceEmpty() throws Exception {
        mockMvc.perform(get("/api/user/byPhoneNo")
                        .header("Authorization", bearerToken)
                        .queryParam("phone_no", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Resource Empty"));
    }

    @Test
    public void meWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void meWithAuthReturnsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/user/me")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(authUser.getId()))
                .andExpect(jsonPath("$.result.phoneNo").value(authUser.getPhoneNo()))
                .andExpect(jsonPath("$.result.email").value(authUser.getEmail()))
                .andExpect(jsonPath("$.result.name").value(authUser.getName()));
    }
}
