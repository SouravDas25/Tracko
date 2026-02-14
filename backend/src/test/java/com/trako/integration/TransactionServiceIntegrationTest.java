package com.trako.integration;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.dtos.TransactionSummaryDTO;
import com.trako.entities.*;
import com.trako.repositories.*;
import com.trako.services.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionServiceIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserCurrencyRepository userCurrencyRepository;

    private User testUser;
    private Account testAccount;
    private Category testCategory;

    @BeforeEach
    public void setup() {
        transactionRepository.deleteAll();
        userCurrencyRepository.deleteAll();
        accountRepository.deleteAll();
        categoryRepository.deleteAll();
        usersRepository.deleteAll();

        testUser = new User();
        testUser.setName("Txn User");
        testUser.setPhoneNo("1112223333");
        testUser.setEmail("txn@example.com");
        testUser.setFireBaseId("pass");
        testUser = usersRepository.save(testUser);

        testAccount = new Account();
        testAccount.setName("Main");
        testAccount.setUserId(testUser.getId());
        testAccount = accountRepository.save(testAccount);

        testCategory = new Category();
        testCategory.setName("Misc");
        testCategory.setUserId(testUser.getId());
        testCategory.setCategoryType(CategoryType.EXPENSE);
        testCategory = categoryRepository.save(testCategory);

        // Mock Security Context for service methods that need loggedInUser()
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(), testUser.getFireBaseId(), Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Date date(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, 12, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    @Test
    public void testFindAllReturnsSortedByDateDesc() {
        // Create 3 transactions with different dates
        createTxn(date(2026, 1, 1), "Oldest");
        createTxn(date(2026, 1, 10), "Newest");
        createTxn(date(2026, 1, 5), "Middle");

        List<Transaction> result = transactionService.findByUserId(testUser.getId());
        assertEquals(3, result.size());
        
        assertEquals("Newest", result.get(0).getName());
        assertEquals("Middle", result.get(1).getName());
        assertEquals("Oldest", result.get(2).getName());
    }

    @Test
    public void testSaveCalculatesAmountFromExchangeRate() {
        Transaction t = new Transaction();
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        t.setName("Foreign Txn");
        t.setDate(new Date());
        t.setTransactionType(1);
        t.setOriginalAmount(100.0);
        t.setExchangeRate(1.5); // 1 Original = 1.5 Base
        t.setOriginalCurrency("EUR");
        // amount is null

        Transaction saved = transactionService.save(t);
        
        assertNotNull(saved.getAmount());
        assertEquals(150.0, saved.getAmount(), 0.01);
    }

    @Test
    public void testSaveCalculatesAmountFromUserCurrencyConfig() {
        // Seed UserCurrency
        UserCurrency uc = new UserCurrency();
        uc.setUser(testUser);
        uc.setCurrencyCode("GBP");
        uc.setExchangeRate(2.0);
        userCurrencyRepository.save(uc);

        Transaction t = new Transaction();
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        t.setName("British Txn");
        t.setDate(new Date());
        t.setTransactionType(1);
        t.setOriginalAmount(50.0);
        t.setOriginalCurrency("GBP");
        // amount and exchangeRate null

        Transaction saved = transactionService.save(t);

        assertNotNull(saved.getAmount());
        assertEquals(100.0, saved.getAmount(), 0.01); // 50 * 2.0
        assertEquals(2.0, saved.getExchangeRate(), 0.01);
    }

    @Test
    public void testGetSummaryCalculations() {
        // Income: 500
        Transaction income = new Transaction();
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setName("Income");
        income.setDate(date(2026, 2, 1));
        income.setTransactionType(2); // Credit
        income.setAmount(500.0);
        income.setIsCountable(1);
        transactionRepository.save(income);

        // Expense: 200
        Transaction expense = new Transaction();
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(testCategory.getId());
        expense.setName("Expense");
        expense.setDate(date(2026, 2, 5));
        expense.setTransactionType(1); // Debit
        expense.setAmount(200.0);
        expense.setIsCountable(1);
        transactionRepository.save(expense);

        // Excluded (not countable): 1000
        Transaction ignored = new Transaction();
        ignored.setAccountId(testAccount.getId());
        ignored.setCategoryId(testCategory.getId());
        ignored.setName("Ignored");
        ignored.setDate(date(2026, 2, 6));
        ignored.setTransactionType(1);
        ignored.setAmount(1000.0);
        ignored.setIsCountable(0);
        transactionRepository.save(ignored);

        Date start = date(2026, 2, 1);
        Date end = date(2026, 3, 1);

        TransactionSummaryDTO summary = transactionService.getSummary(testUser.getId(), start, end);
        
        assertEquals(500.0, summary.getTotalIncome(), 0.01);
        assertEquals(200.0, summary.getTotalExpense(), 0.01);
        assertEquals(300.0, summary.getNetTotal(), 0.01); // 500 - 200
        assertEquals(2, summary.getTransactionCount()); // Ignored one doesn't count
    }

    @Test
    public void testGetSummaryWithRolloverAccumulatesAcrossMultipleMonths() {
        // Jan 2026: Net +100 (income 300, expense 200)
        Transaction janIncome = new Transaction();
        janIncome.setAccountId(testAccount.getId());
        janIncome.setCategoryId(testCategory.getId());
        janIncome.setName("Jan Income");
        janIncome.setDate(date(2026, 1, 5));
        janIncome.setTransactionType(2);
        janIncome.setAmount(300.0);
        janIncome.setIsCountable(1);
        transactionRepository.save(janIncome);

        Transaction janExpense = new Transaction();
        janExpense.setAccountId(testAccount.getId());
        janExpense.setCategoryId(testCategory.getId());
        janExpense.setName("Jan Expense");
        janExpense.setDate(date(2026, 1, 10));
        janExpense.setTransactionType(1);
        janExpense.setAmount(200.0);
        janExpense.setIsCountable(1);
        transactionRepository.save(janExpense);

        // Dec 2025: Net -50 (income 100, expense 150)
        Transaction decIncome = new Transaction();
        decIncome.setAccountId(testAccount.getId());
        decIncome.setCategoryId(testCategory.getId());
        decIncome.setName("Dec Income");
        decIncome.setDate(date(2025, 12, 5));
        decIncome.setTransactionType(2);
        decIncome.setAmount(100.0);
        decIncome.setIsCountable(1);
        transactionRepository.save(decIncome);

        Transaction decExpense = new Transaction();
        decExpense.setAccountId(testAccount.getId());
        decExpense.setCategoryId(testCategory.getId());
        decExpense.setName("Dec Expense");
        decExpense.setDate(date(2025, 12, 10));
        decExpense.setTransactionType(1);
        decExpense.setAmount(150.0);
        decExpense.setIsCountable(1);
        transactionRepository.save(decExpense);

        // Nov 2025: Net +250 (income 400, expense 150)
        Transaction novIncome = new Transaction();
        novIncome.setAccountId(testAccount.getId());
        novIncome.setCategoryId(testCategory.getId());
        novIncome.setName("Nov Income");
        novIncome.setDate(date(2025, 11, 5));
        novIncome.setTransactionType(2);
        novIncome.setAmount(400.0);
        novIncome.setIsCountable(1);
        transactionRepository.save(novIncome);

        Transaction novExpense = new Transaction();
        novExpense.setAccountId(testAccount.getId());
        novExpense.setCategoryId(testCategory.getId());
        novExpense.setName("Nov Expense");
        novExpense.setDate(date(2025, 11, 10));
        novExpense.setTransactionType(1);
        novExpense.setAmount(150.0);
        novExpense.setIsCountable(1);
        transactionRepository.save(novExpense);

        // Feb 2026 current period: Net +300 (income 500, expense 200)
        Transaction febIncome = new Transaction();
        febIncome.setAccountId(testAccount.getId());
        febIncome.setCategoryId(testCategory.getId());
        febIncome.setName("Feb Income");
        febIncome.setDate(date(2026, 2, 1));
        febIncome.setTransactionType(2);
        febIncome.setAmount(500.0);
        febIncome.setIsCountable(1);
        transactionRepository.save(febIncome);

        Transaction febExpense = new Transaction();
        febExpense.setAccountId(testAccount.getId());
        febExpense.setCategoryId(testCategory.getId());
        febExpense.setName("Feb Expense");
        febExpense.setDate(date(2026, 2, 3));
        febExpense.setTransactionType(1);
        febExpense.setAmount(200.0);
        febExpense.setIsCountable(1);
        transactionRepository.save(febExpense);

        Date start = date(2026, 2, 1);
        Date end = date(2026, 3, 1);

        TransactionSummaryDTO summary = transactionService.getSummaryWithRollover(testUser.getId(), start, end, null);

        assertEquals(500.0, summary.getTotalIncome(), 0.01);
        assertEquals(200.0, summary.getTotalExpense(), 0.01);
        assertEquals(300.0, summary.getNetTotal(), 0.01);

        // Rollover should include Nov + Dec + Jan nets
        // Nov: +250, Dec: -50, Jan: +100 => +300
        assertEquals(300.0, summary.getRolloverNet(), 0.01);
        assertEquals(600.0, summary.getNetTotalWithRollover(), 0.01);
    }

    private void createTxn(Date date, String name) {
        Transaction t = new Transaction();
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        t.setName(name);
        t.setDate(date);
        t.setTransactionType(1);
        t.setAmount(10.0);
        t.setIsCountable(1);
        transactionRepository.save(t);
    }
}
