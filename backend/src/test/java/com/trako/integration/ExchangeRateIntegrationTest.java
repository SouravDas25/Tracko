package com.trako.integration;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.exceptions.NotFoundException;
import com.trako.models.external.ExchangeRateApiResponse;
import com.trako.repositories.UsersRepository;
import com.trako.services.ExchangeRateService;
import com.trako.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
public class ExchangeRateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UsersRepository usersRepository;

    private String bearerToken;

    @BeforeEach
    public void setup() {
        usersRepository.deleteAll();
        var user = new com.trako.entities.User();
        user.setName("X");
        user.setPhoneNo("9999999999");
        user.setEmail("x@example.com");
        user.setPassword("pass");
        usersRepository.save(user);
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                user.getPhoneNo(), user.getPassword(), Collections.emptyList());
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);
    }

    @Test
    public void getRatesSuccess() throws Exception {
        Map<String, Double> rates = new HashMap<>();
        rates.put("EUR", 0.9);
        rates.put("INR", 83.0);
        ExchangeRateApiResponse payload = new ExchangeRateApiResponse("USD", rates);

        when(exchangeRateService.getRates("USD")).thenReturn(payload);

        mockMvc.perform(get("/api/exchange-rates/USD")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.baseCode").value("USD"))
                .andExpect(jsonPath("$.result.rates.EUR").value(0.9))
                .andExpect(jsonPath("$.result.rates.INR").value(83.0));
    }

    @Test
    public void getRatesNotFoundWhenServiceThrowsException() throws Exception {
        when(exchangeRateService.getRates("ZZZ")).thenThrow(new NotFoundException("Could not fetch exchange rates"));

        mockMvc.perform(get("/api/exchange-rates/ZZZ")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }
}
