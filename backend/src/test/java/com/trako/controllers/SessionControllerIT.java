package com.trako.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.entities.User;
import com.trako.repositories.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SessionControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private com.trako.repositories.BudgetCategoryAllocationRepository budgetCategoryAllocationRepository;

    @Autowired
    private com.trako.repositories.TransactionRepository transactionRepository;

    @Autowired
    private com.trako.repositories.CategoryRepository categoryRepository;

    @Autowired
    private com.trako.repositories.BudgetMonthRepository budgetMonthRepository;

    @Autowired
    private com.trako.repositories.AccountRepository accountRepository;

    @Autowired
    private com.trako.repositories.SplitRepository splitRepository;

    @Autowired
    private com.trako.repositories.ContactRepository contactRepository;

    @Autowired
    private com.trako.repositories.AllocationRuleRepository allocationRuleRepository;

    @BeforeEach
    void setUp() {
        splitRepository.deleteAll();
        budgetCategoryAllocationRepository.deleteAll();
        transactionRepository.deleteAll();
        allocationRuleRepository.deleteAll();
        contactRepository.deleteAll();
        accountRepository.deleteAll();
        budgetMonthRepository.deleteAll();
        categoryRepository.deleteAll();
        usersRepository.deleteAll();
    }

    @Test
    void login_withValidDevUser_returnsToken() throws Exception {
        User user = new User();
        user.setName("user");
        user.setPhoneNo("user");
        user.setFireBaseId("password");
        usersRepository.save(user);

        Map<String, Object> body = new HashMap<>();
        body.put("username", "user");
        body.put("password", "password");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_withInvalidPassword_returnsUnauthorized() throws Exception {
        User user = new User();
        user.setName("user");
        user.setPhoneNo("user");
        user.setFireBaseId("password");
        usersRepository.save(user);

        Map<String, Object> body = new HashMap<>();
        body.put("username", "user");
        body.put("password", "wrong");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}
