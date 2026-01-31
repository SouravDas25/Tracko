package com.trako.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.entities.Transaction;
import com.trako.services.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    private Transaction testTransaction;

    @BeforeEach
    public void setup() {
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setTransactionType(1);
        testTransaction.setName("Lunch");
        testTransaction.setAmount(25.50);
        testTransaction.setDate(new Date());
        testTransaction.setAccountId(1L);
        testTransaction.setCategoryId(1L);
    }

    @Test
    @WithMockUser
    public void testGetAll() throws Exception {
        when(transactionService.findAll()).thenReturn(Arrays.asList(testTransaction));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Lunch"));

        verify(transactionService, times(1)).findAll();
    }

    @Test
    @WithMockUser
    public void testGetById() throws Exception {
        when(transactionService.findById(1L)).thenReturn(Optional.of(testTransaction));

        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Lunch"));

        verify(transactionService, times(1)).findById(1L);
    }

    @Test
    @WithMockUser
    public void testGetByUserId() throws Exception {
        when(transactionService.findByUserId("user123")).thenReturn(Arrays.asList(testTransaction));

        mockMvc.perform(get("/api/transactions/user/user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Lunch"));

        verify(transactionService, times(1)).findByUserId("user123");
    }

    @Test
    @WithMockUser
    public void testCreate() throws Exception {
        when(transactionService.save(any(Transaction.class))).thenReturn(testTransaction);

        mockMvc.perform(post("/api/transactions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTransaction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Lunch"));

        verify(transactionService, times(1)).save(any(Transaction.class));
    }

    @Test
    @WithMockUser
    public void testDelete() throws Exception {
        doNothing().when(transactionService).delete(1L);

        mockMvc.perform(delete("/api/transactions/1")
                .with(csrf()))
                .andExpect(status().isOk());

        verify(transactionService, times(1)).delete(1L);
    }
}
