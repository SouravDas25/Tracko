package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Frequency;
import com.trako.entities.RecurringTransaction;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.RecurringTransactionRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UsersRepository;
import com.trako.services.RecurringTransactionService;
import com.trako.util.JwtTokenUtil;
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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class RecurringTransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RecurringTransactionService recurringTransactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private User testUser;
    private Account testAccount;
    private Category testCategory;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        recurringTransactionRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        categoryRepository.deleteAll();
        usersRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        testUser.setFireBaseId("firebase_id_123"); // Set firebaseId as it is used as password in principal
        testUser = usersRepository.save(testUser);

        var principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(),
                testUser.getFireBaseId(),
                java.util.Collections.emptyList()
        );
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);

        testAccount = new Account();
        testAccount.setName("Test Account");
        testAccount.setUserId(testUser.getId());
        testAccount.setCurrency("INR");
        testAccount = accountRepository.save(testAccount);

        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setUserId(testUser.getId());
        testCategory.setCategoryType(com.trako.entities.CategoryType.EXPENSE); // Fixed: setType(int) -> setCategoryType(CategoryType)
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    public void testCreateRecurringTransaction() throws Exception {
        RecurringTransaction rt = new RecurringTransaction();
        rt.setName("Netflix Subscription");
        rt.setAmount(199.0);
        rt.setAccountId(testAccount.getId());
        rt.setCategoryId(testCategory.getId());
        rt.setTransactionType(1);
        rt.setFrequency(Frequency.MONTHLY);
        rt.setStartDate(new Date());
        rt.setNextRunDate(new Date());

        mockMvc.perform(post("/api/recurring-transactions")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").exists())
                .andExpect(jsonPath("$.result.name").value("Netflix Subscription"));

        assertEquals(1, recurringTransactionRepository.count());
    }

    @Test
    public void testCreateRecurringTransactionWithCurrency() throws Exception {
        RecurringTransaction rt = new RecurringTransaction();
        rt.setName("Netflix Subscription USD");
        rt.setAmount(1200.0); // Base amount in INR (approx)
        rt.setAccountId(testAccount.getId());
        rt.setCategoryId(testCategory.getId());
        rt.setTransactionType(1);
        rt.setFrequency(Frequency.MONTHLY);
        rt.setStartDate(new Date());
        rt.setNextRunDate(new Date());
        
        // Currency fields
        rt.setOriginalCurrency("USD");
        rt.setOriginalAmount(15.0);
        rt.setExchangeRate(80.0);

        mockMvc.perform(post("/api/recurring-transactions")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").exists())
                .andExpect(jsonPath("$.result.name").value("Netflix Subscription USD"))
                .andExpect(jsonPath("$.result.originalCurrency").value("USD"))
                .andExpect(jsonPath("$.result.originalAmount").value(15.0))
                .andExpect(jsonPath("$.result.exchangeRate").value(80.0));

        assertEquals(1, recurringTransactionRepository.count());
        RecurringTransaction saved = recurringTransactionRepository.findAll().get(0);
        assertEquals("USD", saved.getOriginalCurrency());
        assertEquals(15.0, saved.getOriginalAmount());
        assertEquals(80.0, saved.getExchangeRate());
    }

    @Test
    public void testProcessDueTransactions() {
        // Create a recurring transaction that is due (nextRunDate in the past)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = cal.getTime();

        RecurringTransaction rt = new RecurringTransaction();
        rt.setUserId(testUser.getId());
        rt.setName("Due Transaction");
        rt.setAmount(500.0);
        rt.setAccountId(testAccount.getId());
        rt.setCategoryId(testCategory.getId());
        rt.setTransactionType(1);
        rt.setFrequency(Frequency.MONTHLY);
        rt.setStartDate(yesterday);
        rt.setNextRunDate(yesterday);
        rt.setIsActive(true);
        recurringTransactionRepository.save(rt);

        // Run the processing logic
        recurringTransactionService.processDueTransactions();

        // Verify a transaction was created
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(1, transactions.size());
        Transaction t = transactions.get(0);
        assertEquals("Due Transaction", t.getName());
        assertEquals(500.0, t.getAmount());

        // Verify recurring transaction was updated
        RecurringTransaction updatedRt = recurringTransactionRepository.findById(rt.getId()).orElseThrow();
        assertNotNull(updatedRt.getLastRunDate());
        
        // Next run date should be yesterday + 1 month
        cal.setTime(yesterday);
        cal.add(Calendar.MONTH, 1);
        Date expectedNextRun = cal.getTime();
        
        // Allow small difference in milliseconds if any, but logic uses Calendar math so should be close
        // Actually the logic sets it exactly using Calendar add
        assertEquals(expectedNextRun.toString(), updatedRt.getNextRunDate().toString());
    }

    @Test
    public void testRecurringTransferCreation() {
        // Create a second account for transfer
        Account targetAccount = new Account();
        targetAccount.setName("Savings");
        targetAccount.setUserId(testUser.getId());
        targetAccount = accountRepository.save(targetAccount);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = cal.getTime();

        RecurringTransaction rt = new RecurringTransaction();
        rt.setUserId(testUser.getId());
        rt.setName("Monthly Savings");
        rt.setAmount(1000.0);
        rt.setAccountId(testAccount.getId());
        rt.setToAccountId(targetAccount.getId()); // Transfer
        rt.setCategoryId(testCategory.getId()); // Usually handled by service but field is required
        rt.setTransactionType(3); // Transfer
        rt.setFrequency(Frequency.MONTHLY);
        rt.setStartDate(yesterday);
        rt.setNextRunDate(yesterday);
        rt.setIsActive(true);
        recurringTransactionRepository.save(rt);

        // Run processing
        recurringTransactionService.processDueTransactions();

        // Verify transfer transactions created (Debit and Credit)
        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(2, transactions.size());
        
        boolean hasDebit = transactions.stream().anyMatch(t -> t.getAmount() == 1000.0 && t.getTransactionType() == 1);
        boolean hasCredit = transactions.stream().anyMatch(t -> t.getAmount() == 1000.0 && t.getTransactionType() == 2);
        
        assertTrue(hasDebit);
        assertTrue(hasCredit);
    }
}
