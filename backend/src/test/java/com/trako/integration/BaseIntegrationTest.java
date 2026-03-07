package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.User;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UsersRepository;
import com.trako.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UsersRepository usersRepository;

    @Autowired
    protected AccountRepository accountRepository;

    @Autowired
    protected CategoryRepository categoryRepository;

    @Autowired
    protected TransactionRepository transactionRepository;

    @Autowired
    protected JwtTokenUtil jwtTokenUtil;

    protected User createUniqueUser() {
        return createUniqueUser("Test User");
    }

    protected User createUniqueUser(String name) {
        User user = new User();
        user.setName(name);
        user.setPhoneNo(generateUniquePhone());
        user.setEmail("test_" + UUID.randomUUID() + "@example.com");
        user.setPassword("password");
        user.setBaseCurrency("INR");
        return usersRepository.save(user);
    }

    protected String generateBearerToken(User user) {
        var principal = new org.springframework.security.core.userdetails.User(
                user.getPhoneNo(),
                user.getPassword(),
                Collections.emptyList()
        );
        return "Bearer " + jwtTokenUtil.generateToken(principal);
    }

    protected String generateUniquePhone() {
        long base = Math.abs(System.nanoTime());
        // Ensure 10 digits [1,000,000,000 - 9,999,999,999]
        long tenDigits = (base % 9_000_000_000L) + 1_000_000_000L;
        return String.valueOf(tenDigits);
    }
}
