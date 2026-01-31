package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestSecurityConfig;
import com.trako.entities.Account;
import com.trako.entities.User;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
public class AccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UsersRepository usersRepository;

    private User testUser;

    @BeforeEach
    public void setup() {
        accountRepository.deleteAll();
        usersRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        testUser = usersRepository.save(testUser);
    }

    @Test
    public void testCreateAccount() throws Exception {
        Account account = new Account();
        account.setName("Savings");
        account.setUserId(testUser.getId());

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(account)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Savings"))
                .andExpect(jsonPath("$.result.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.result.id").isNotEmpty());
    }

    @Test
    public void testGetAllAccounts() throws Exception {
        Account account1 = new Account();
        account1.setName("Savings");
        account1.setUserId(testUser.getId());
        accountRepository.save(account1);

        Account account2 = new Account();
        account2.setName("Cash");
        account2.setUserId(testUser.getId());
        accountRepository.save(account2);

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[*].name", containsInAnyOrder("Savings", "Cash")));
    }

    @Test
    public void testGetAccountById() throws Exception {
        Account account = new Account();
        account.setName("Checking");
        account.setUserId(testUser.getId());
        Account saved = accountRepository.save(account);

        mockMvc.perform(get("/api/accounts/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Checking"))
                .andExpect(jsonPath("$.result.id").value(saved.getId()));
    }

    @Test
    public void testGetAccountsByUserId() throws Exception {
        Account account1 = new Account();
        account1.setName("Savings");
        account1.setUserId(testUser.getId());
        accountRepository.save(account1);

        Account account2 = new Account();
        account2.setName("Investment");
        account2.setUserId(testUser.getId());
        accountRepository.save(account2);

        mockMvc.perform(get("/api/accounts/user/" + testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[*].userId", everyItem(is(testUser.getId()))));
    }

    @Test
    public void testUpdateAccount() throws Exception {
        Account account = new Account();
        account.setName("Old Name");
        account.setUserId(testUser.getId());
        Account saved = accountRepository.save(account);

        saved.setName("New Name");

        mockMvc.perform(put("/api/accounts/" + saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("New Name"))
                .andExpect(jsonPath("$.result.id").value(saved.getId()));
    }

    @Test
    public void testDeleteAccount() throws Exception {
        Account account = new Account();
        account.setName("To Delete");
        account.setUserId(testUser.getId());
        Account saved = accountRepository.save(account);

        mockMvc.perform(delete("/api/accounts/" + saved.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts/" + saved.getId()))
                .andExpect(status().isNotFound());
    }
}
