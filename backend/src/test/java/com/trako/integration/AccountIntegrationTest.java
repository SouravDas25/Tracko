package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.Account;
import com.trako.models.request.AccountSaveRequest;
import com.trako.entities.User;
import com.trako.repositories.AccountRepository;
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
public class AccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private User testUser;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        accountRepository.deleteAll();
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
    public void testCreateAccountWithoutNameReturnsBadRequest() throws Exception {
        AccountSaveRequest req = new AccountSaveRequest();
        // name is null -> @NotNull should trigger 400

        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateAccountWithoutNameReturnsBadRequest() throws Exception {
        Account saved = new Account();
        saved.setName("HasName");
        saved.setUserId(testUser.getId());
        saved = accountRepository.save(saved);

        AccountSaveRequest req = new AccountSaveRequest();
        // name not set -> should 400

        mockMvc.perform(put("/api/accounts/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateAccountWithoutAuthReturnsUnauthorized() throws Exception {
        AccountSaveRequest req = new AccountSaveRequest();
        req.setName("NoAuth");

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetAccountsByOtherUserUnauthorized() throws Exception {
        // Another user id
        User other = new User();
        other.setName("OtherU");
        other.setPhoneNo("2002002000");
        other.setEmail("otheru@example.com");
        other.setFireBaseId("other_pass");
        other = usersRepository.save(other);

        mockMvc.perform(get("/api/accounts/user/" + other.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetAccountByIdUnauthorizedForForeignUser() throws Exception {
        User other = new User();
        other.setName("OtherAcc");
        other.setPhoneNo("3003003000");
        other.setEmail("otheracc@example.com");
        other.setFireBaseId("otheracc_pass");
        other = usersRepository.save(other);

        Account foreign = new Account();
        foreign.setName("Foreign");
        foreign.setUserId(other.getId());
        foreign = accountRepository.save(foreign);

        mockMvc.perform(get("/api/accounts/" + foreign.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateAccountUnauthorizedForForeignUser() throws Exception {
        User other = new User();
        other.setName("UpdOtherAcc");
        other.setPhoneNo("4004004000");
        other.setEmail("updother@example.com");
        other.setFireBaseId("updother_pass");
        other = usersRepository.save(other);

        Account foreign = new Account();
        foreign.setName("ForeignUpd");
        foreign.setUserId(other.getId());
        foreign = accountRepository.save(foreign);

        AccountSaveRequest req = new AccountSaveRequest();
        req.setName("TryUpdate");

        mockMvc.perform(put("/api/accounts/" + foreign.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeleteAccountUnauthorizedForForeignUser() throws Exception {
        User other = new User();
        other.setName("DelOtherAcc");
        other.setPhoneNo("5005005000");
        other.setEmail("delother@example.com");
        other.setFireBaseId("delother_pass");
        other = usersRepository.save(other);

        Account foreign = new Account();
        foreign.setName("ForeignDel");
        foreign.setUserId(other.getId());
        foreign = accountRepository.save(foreign);

        mockMvc.perform(delete("/api/accounts/" + foreign.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetAccountByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/accounts/999999")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateAccountNotFound() throws Exception {
        AccountSaveRequest req = new AccountSaveRequest();
        req.setName("UpdateMissing");

        mockMvc.perform(put("/api/accounts/999999")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateAccount() throws Exception {
        Account account = new Account();
        account.setName("Savings");
        account.setUserId(testUser.getId());

        mockMvc.perform(post("/api/accounts")
                .header("Authorization", bearerToken)
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

        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", bearerToken))
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

        mockMvc.perform(get("/api/accounts/" + saved.getId())
                        .header("Authorization", bearerToken))
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

        mockMvc.perform(get("/api/accounts/user/" + testUser.getId())
                        .header("Authorization", bearerToken))
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
                .header("Authorization", bearerToken)
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

        mockMvc.perform(delete("/api/accounts/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }
}
