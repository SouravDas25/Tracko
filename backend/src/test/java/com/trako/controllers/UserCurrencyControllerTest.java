package com.trako.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.entities.User;
import com.trako.entities.UserCurrency;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.models.request.UserCurrencyRequest;
import com.trako.services.CurrencyService;
import com.trako.services.JwtUserDetailsService;
import com.trako.services.UserService;
import com.trako.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserCurrencyController.class)
public class UserCurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private CurrencyService currencyService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private JwtUserDetailsService jwtUserDetailsService;

    private User testUser;
    private UserCurrency testUserCurrency;

    @BeforeEach
    public void setup() throws Exception {
        testUser = new User();
        testUser.setId("user123");
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");

        testUserCurrency = new UserCurrency();
        testUserCurrency.setId(123L);
        testUserCurrency.setUser(testUser);
        testUserCurrency.setCurrencyCode("USD");
        testUserCurrency.setExchangeRate(83.5);

        when(userService.loggedInUser()).thenReturn(testUser);
    }

    @Test
    @WithMockUser
    public void testGetAll() throws Exception {
        when(currencyService.getAll("user123")).thenReturn(Collections.singletonList(testUserCurrency));

        mockMvc.perform(get("/api/user-currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].currencyCode").value("USD"))
                .andExpect(jsonPath("$.result[0].exchangeRate").value(83.5));

        verify(currencyService, times(1)).getAll("user123");
    }

    @Test
    @WithMockUser
    public void testSaveNewCurrency() throws Exception {
        UserCurrencyRequest request = new UserCurrencyRequest();
        request.setCurrencyCode("EUR");
        request.setExchangeRate(90.0);
        when(currencyService.save(any(User.class), eq("EUR"), eq(90.0))).thenReturn(new UserCurrency());

        mockMvc.perform(post("/api/user-currencies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Saved"));
        verify(currencyService, times(1)).save(any(User.class), eq("EUR"), eq(90.0));
    }

    @Test
    @WithMockUser
    public void testUpdateExistingCurrency() throws Exception {
        UserCurrencyRequest request = new UserCurrencyRequest();
        request.setCurrencyCode("USD");
        request.setExchangeRate(84.0);
        when(currencyService.save(any(User.class), eq("USD"), eq(84.0))).thenReturn(testUserCurrency);

        mockMvc.perform(post("/api/user-currencies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Saved"));
        verify(currencyService, times(1)).save(any(User.class), eq("USD"), eq(84.0));
    }

    @Test
    @WithMockUser
    public void testDeleteCurrency() throws Exception {
        doNothing().when(currencyService).delete("user123", "USD"); // delete still uses ID in controller? Let's check controller.

        mockMvc.perform(delete("/api/user-currencies/USD")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("Deleted"));
        // The controller was updated to: currencyService.delete(user.getId(), code);
        // Wait, did I update delete to use User object in Controller?
        // Let's check previous turn.
        // Yes: currencyService.delete(user.getId(), code); in controller patch.
        // And CurrencyService has: delete(String userId, String currencyCode).
        // So delete(String, String) is correct.
        verify(currencyService, times(1)).delete("user123", "USD");
    }

    @Test
    @WithMockUser
    public void testGetAllUnauthorized() throws Exception {
        when(userService.loggedInUser()).thenThrow(new UserNotLoggedInException());

        mockMvc.perform(get("/api/user-currencies"))
                .andExpect(status().isUnauthorized());
    }
}
