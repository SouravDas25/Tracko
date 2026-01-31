package com.trako.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.entities.Account;
import com.trako.services.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    private Account testAccount;

    @BeforeEach
    public void setup() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setName("Savings");
        testAccount.setUserId("user123");
    }

    @Test
    @WithMockUser
    public void testGetAll() throws Exception {
        when(accountService.findAll()).thenReturn(Arrays.asList(testAccount));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Savings"));

        verify(accountService, times(1)).findAll();
    }

    @Test
    @WithMockUser
    public void testGetById() throws Exception {
        when(accountService.findById(1L)).thenReturn(Optional.of(testAccount));

        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Savings"));

        verify(accountService, times(1)).findById(1L);
    }

    @Test
    @WithMockUser
    public void testGetByUserId() throws Exception {
        when(accountService.findByUserId("user123")).thenReturn(Arrays.asList(testAccount));

        mockMvc.perform(get("/api/accounts/user/user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value("user123"));

        verify(accountService, times(1)).findByUserId("user123");
    }

    @Test
    @WithMockUser
    public void testCreate() throws Exception {
        when(accountService.save(any(Account.class))).thenReturn(testAccount);

        mockMvc.perform(post("/api/accounts")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAccount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Savings"));

        verify(accountService, times(1)).save(any(Account.class));
    }

    @Test
    @WithMockUser
    public void testUpdate() throws Exception {
        when(accountService.save(any(Account.class))).thenReturn(testAccount);

        mockMvc.perform(put("/api/accounts/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testAccount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Savings"));

        verify(accountService, times(1)).save(any(Account.class));
    }

    @Test
    @WithMockUser
    public void testDelete() throws Exception {
        doNothing().when(accountService).delete(1L);

        mockMvc.perform(delete("/api/accounts/1")
                .with(csrf()))
                .andExpect(status().isOk());

        verify(accountService, times(1)).delete(1L);
    }
}
