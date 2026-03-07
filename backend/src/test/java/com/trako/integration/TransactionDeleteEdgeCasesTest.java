package com.trako.integration;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.*;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UsersRepository;
import com.trako.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionDeleteEdgeCasesTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private String tokenA;
    private String tokenB;
    private User userA;
    private User userB;
    private Account accA;
    private Category catA;

    @BeforeEach
    public void setup() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        usersRepository.deleteAll();

        userA = new User();
        userA.setName("A");
        userA.setPhoneNo("5100000001");
        userA.setEmail("a@x.com");
        userA.setPassword("p");
        userA = usersRepository.save(userA);
        userB = new User();
        userB.setName("B");
        userB.setPhoneNo("5100000002");
        userB.setEmail("b@x.com");
        userB.setPassword("p");
        userB = usersRepository.save(userB);

        var pA = new org.springframework.security.core.userdetails.User(userA.getPhoneNo(), userA.getPassword(), Collections.emptyList());
        var pB = new org.springframework.security.core.userdetails.User(userB.getPhoneNo(), userB.getPassword(), Collections.emptyList());
        tokenA = "Bearer " + jwtTokenUtil.generateToken(pA);
        tokenB = "Bearer " + jwtTokenUtil.generateToken(pB);

        accA = new Account();
        accA.setName("A1");
        accA.setUserId(userA.getId());
        accA = accountRepository.save(accA);
        catA = new Category();
        catA.setName("Food");
        catA.setUserId(userA.getId());
        catA = categoryRepository.save(catA);
    }

    private Transaction createTransactionForUserA() {
        Transaction t = new Transaction();
        t.setTransactionType(TransactionType.DEBIT);
        t.setName("T");
        t.setOriginalAmount(10.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setDate(new Date());
        t.setAccountId(accA.getId());
        t.setCategoryId(catA.getId());
        t.setIsCountable(1);
        return transactionRepository.save(t);
    }

    @Test
    public void delete_notFound_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/transactions/999999")
                        .header("Authorization", tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    public void delete_unauthorized_returnsUnauthorized() throws Exception {
        Transaction t = createTransactionForUserA();
        mockMvc.perform(delete("/api/transactions/" + t.getId())
                        .header("Authorization", tokenB))
                .andExpect(status().isUnauthorized());
    }
}
