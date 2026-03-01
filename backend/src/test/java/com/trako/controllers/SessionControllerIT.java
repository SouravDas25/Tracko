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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

// IMPORTANT: Keep Liquibase ENABLED for tests. Do NOT disable it to "fix" checksum issues.
// Instead, we:
//  - run with the 'test' profile (isolated in-memory DB)
//  - drop schema first and clear Liquibase checksums on startup
// This guarantees a clean schema per run AND validates migrations correctly.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        user.setPassword(passwordEncoder.encode("password"));
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
        user.setPassword(passwordEncoder.encode("password"));
        usersRepository.save(user);

        Map<String, Object> body = new HashMap<>();
        body.put("username", "user");
        body.put("password", "wrong");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createUser_viaApi_hashesPassword_and_allowsLogin() throws Exception {
        // Seed an admin user directly
        User admin = new User();
        admin.setName("admin");
        admin.setPhoneNo("0000000001");
        admin.setIsAdmin(1);
        admin.setPassword(passwordEncoder.encode("adminpass"));
        usersRepository.save(admin);

        // Login as admin to get JWT token
        Map<String, Object> adminLogin = new HashMap<>();
        adminLogin.put("username", "0000000001");
        adminLogin.put("password", "adminpass");

        String adminToken = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract token value from {"token":"..."}
        String jwt = objectMapper.readTree(adminToken).get("token").asText();

        // Create a new user via API (so backend encodes password)
        Map<String, Object> createUser = new HashMap<>();
        createUser.put("name", "user");
        createUser.put("phoneNo", "0000000002");
        createUser.put("password", "password");
        createUser.put("baseCurrency", "INR");

        mockMvc.perform(post("/api/user/create")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUser)))
                .andExpect(status().isOk());

        // Assert password stored as hash (not raw) and matches via encoder
        User created = usersRepository.findByPhoneNo("0000000002");
        assertNotNull(created);
        assertNotNull(created.getPassword());
        assertNotEquals("password", created.getPassword());
        assertTrue(created.getPassword().startsWith("$2")); // BCrypt prefix ($2a/$2b)
        assertTrue(passwordEncoder.matches("password", created.getPassword()));

        // Verify login works for the newly created user
        Map<String, Object> userLogin = new HashMap<>();
        userLogin.put("username", "0000000002");
        userLogin.put("password", "password");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
