package com.trako.services;

import com.trako.entities.*;
import com.trako.enums.TransactionDbType;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UsersRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TransactionRepositoryAmountNullTest {

    private static final boolean schemaPatched = false;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private User user;
    private Account account;
    private Category category;

    @BeforeEach
    public void setup() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        usersRepository.deleteAll();

        user = new User();
        user.setName("Tx User");
        user.setPhoneNo("1234500000");
        user.setEmail("tx@example.com");
        user.setPassword("pass");
        user = usersRepository.save(user);

        account = new Account();
        account.setName("RepoTest");
        account.setUserId(user.getId());
        account = accountRepository.save(account);

        category = new Category();
        category.setName("Misc");
        category.setUserId(user.getId());
        category = categoryRepository.save(category);
    }

    @Test
    public void testDirectRepositorySave_thenFindById_amountIsNull() {
        Transaction t = new Transaction();
        t.setAccountId(account.getId());
        t.setCategoryId(category.getId());
        t.setName("Repo Path");
        t.setDate(new Date());
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setOriginalAmount(10.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setIsCountable(1);


        Transaction persisted = transactionRepository.saveAndFlush(t);
        assertNotNull(persisted.getId());

        assertNotNull(persisted.getAmount(), "Amount should be populated after refresh");
        assertEquals(10.0, persisted.getAmount());
    }
}
