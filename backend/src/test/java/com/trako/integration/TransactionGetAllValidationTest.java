package com.trako.integration;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionGetAllValidationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsersRepository usersRepository;
    @Autowired private JwtTokenUtil jwtTokenUtil;

    private String bearerToken;

    @BeforeEach
    public void setup() {
        usersRepository.deleteAll();
        User u = new User();
        u.setName("U1");
        u.setPhoneNo("1111111111");
        u.setEmail("u1@example.com");
        u.setPassword("pass");
        u = usersRepository.save(u);

        var principal = new org.springframework.security.core.userdetails.User(
                u.getPhoneNo(), u.getPassword(), Collections.emptyList());
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);
    }

    @Test
    public void getAll_pageNegative_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("page", "-1")
                        .param("month", "1")
                        .param("year", "2025"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getAll_sizeZero_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("size", "0")
                        .param("month", "1")
                        .param("year", "2025"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getAll_sizeTooLarge_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("size", "10001")
                        .param("month", "1")
                        .param("year", "2025"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getAll_monthOutOfRange_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", "13")
                        .param("year", "2025"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getAll_missingMonthAndDates_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getAll_onlyStartDateProvided_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2025-01-01"))
                .andExpect(status().isBadRequest());
    }
}
