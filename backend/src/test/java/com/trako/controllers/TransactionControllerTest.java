package com.trako.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.entities.Account;
import com.trako.entities.Transaction;
import com.trako.entities.TransactionEntryType;
import com.trako.entities.User;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.UserCurrencyRepository;
import com.trako.services.JwtUserDetailsService;
import com.trako.services.UserService;
import com.trako.services.transactions.TransactionService;
import com.trako.services.transactions.TransactionWriteService;
import com.trako.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private TransactionWriteService transactionWriteService;

    @MockBean
    private UserService userService;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private UserCurrencyRepository userCurrencyRepository;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private JwtUserDetailsService jwtUserDetailsService;

    private Transaction testTransaction;
    private User testUser;

    @BeforeEach
    public void setup() throws Exception {
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setTransactionType(TransactionEntryType.DEBIT);
        testTransaction.setName("Lunch");
        testTransaction.setOriginalAmount(25.50);
        testTransaction.setOriginalCurrency("INR");
        testTransaction.setExchangeRate(1.0);
        testTransaction.setDate(new Date());
        testTransaction.setAccountId(1L);
        testTransaction.setCategoryId(1L);

        testUser = new User();
        testUser.setId("user123");
        testUser.setName("Test User");

        Account testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUserId("user123");

        com.trako.entities.Category testCategory = new com.trako.entities.Category();
        testCategory.setId(1L);
        testCategory.setUserId("user123");

        when(userService.loggedInUser()).thenReturn(testUser);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
    }

    @Test
    @WithMockUser
    public void testGetAll() throws Exception {
        when(transactionService.findByUserIdAndDateBetween(anyString(), any(Date.class), any(Date.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(testTransaction)));

        mockMvc.perform(get("/api/transactions")
                        .param("month", "1")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactions[0].name").value("Lunch"));

        verify(transactionService, times(1))
                .findByUserIdAndDateBetween(anyString(), any(Date.class), any(Date.class), any(Pageable.class));
    }

    @Test
    @WithMockUser
    public void testGetById() throws Exception {
        when(transactionService.findById(1L)).thenReturn(Optional.of(testTransaction));

        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Lunch"));

        verify(transactionService, times(1)).findById(1L);
    }


    @Test
    @WithMockUser
    public void testCreate() throws Exception {
        when(transactionWriteService.createUnifiedTransaction(anyString(), any(com.trako.models.request.TransactionRequest.class)))
                .thenReturn(testTransaction);

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testTransaction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Lunch"));

        verify(transactionWriteService, times(1))
                .createUnifiedTransaction(anyString(), any(com.trako.models.request.TransactionRequest.class));
    }

    @Test
    @WithMockUser
    public void testDelete() throws Exception {
        doNothing().when(transactionWriteService).deleteUnifiedTransaction(anyString(), eq(1L));

        mockMvc.perform(delete("/api/transactions/1")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(transactionWriteService, times(1)).deleteUnifiedTransaction(anyString(), eq(1L));
    }
}
