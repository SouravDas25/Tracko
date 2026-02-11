package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.User;
import com.trako.entities.UserCurrency;
import com.trako.models.request.UserCurrencyRequest;
import com.trako.repositories.UserCurrencyRepository;
import com.trako.repositories.UsersRepository;
import com.trako.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class UserCurrencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private UserCurrencyRepository userCurrencyRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private User testUser;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        userCurrencyRepository.deleteAll();
        usersRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        testUser.setFireBaseId("password");
        testUser = usersRepository.save(testUser);

        UserDetails principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(),
                testUser.getFireBaseId(),
                Collections.emptyList()
        );
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);
    }

    @Test
    public void getAllWithoutAuthIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user-currencies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createWithoutAuthIsUnauthorized() throws Exception {
        UserCurrencyRequest req = new UserCurrencyRequest();
        req.setCurrencyCode("EUR");
        req.setExchangeRate(0.9);

        mockMvc.perform(post("/api/user-currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteWithoutAuthIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/user-currencies/EUR"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createAndListUserCurrencies() throws Exception {
        UserCurrencyRequest req = new UserCurrencyRequest();
        req.setCurrencyCode("EUR");
        req.setExchangeRate(0.9);

        mockMvc.perform(post("/api/user-currencies")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Saved successfully"));

        mockMvc.perform(get("/api/user-currencies")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].currencyCode").value("EUR"))
                .andExpect(jsonPath("$.result[0].exchangeRate").value(0.9));
    }

    @Test
    public void duplicateCreateUpdatesExchangeRate() throws Exception {
        // initial save
        UserCurrencyRequest req = new UserCurrencyRequest();
        req.setCurrencyCode("JPY");
        req.setExchangeRate(150.0);
        mockMvc.perform(post("/api/user-currencies")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // update same code -> should update rate
        UserCurrencyRequest update = new UserCurrencyRequest();
        update.setCurrencyCode("JPY");
        update.setExchangeRate(151.5);
        mockMvc.perform(post("/api/user-currencies")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user-currencies")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].currencyCode").value("JPY"))
                .andExpect(jsonPath("$.result[0].exchangeRate").value(151.5));
    }

    @Test
    public void deleteCurrency() throws Exception {
        // seed one currency for the user via controller POST for consistent state
        UserCurrencyRequest req = new UserCurrencyRequest();
        req.setCurrencyCode("GBP");
        req.setExchangeRate(0.8);
        mockMvc.perform(post("/api/user-currencies")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/user-currencies/GBP")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted successfully"));

        mockMvc.perform(get("/api/user-currencies")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(0)));
    }

    @Test
    public void createWithMissingFieldsReturnsOkPerCurrentControllerBehavior() throws Exception {
        // missing currencyCode
        UserCurrencyRequest noCode = new UserCurrencyRequest();
        noCode.setExchangeRate(1.0);
        mockMvc.perform(post("/api/user-currencies")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noCode)))
                .andExpect(status().isBadRequest());

        // missing exchangeRate
        UserCurrencyRequest noRate = new UserCurrencyRequest();
        noRate.setCurrencyCode("CAD");
        mockMvc.perform(post("/api/user-currencies")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noRate)))
                .andExpect(status().isBadRequest());
    }
}
