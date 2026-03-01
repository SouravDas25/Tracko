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

import java.util.Calendar;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionGetAllMalformedAccountIdsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsersRepository usersRepository;
    @Autowired private JwtTokenUtil jwtTokenUtil;

    private String bearerToken;

    @BeforeEach
    public void setup() {
        usersRepository.deleteAll();
        User u = new User();
        u.setName("U1");
        u.setPhoneNo("8100000000");
        u.setEmail("u1@example.com");
        u.setPassword("pass");
        u = usersRepository.save(u);

        var principal = new org.springframework.security.core.userdetails.User(
                u.getPhoneNo(), u.getPassword(), Collections.emptyList());
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);
    }

    @Test
    public void getAll_malformedAccountIds_parsesGracefully_returnsOk() throws Exception {
        Calendar cal = Calendar.getInstance();
        int m = cal.get(Calendar.MONTH) + 1;
        int y = cal.get(Calendar.YEAR);
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", String.valueOf(m))
                        .param("year", String.valueOf(y))
                        .param("accountIds", "abc,123, ,xyz"))
                .andExpect(status().isOk());
    }
}
