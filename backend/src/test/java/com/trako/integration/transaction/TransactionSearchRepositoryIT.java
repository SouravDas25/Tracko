package com.trako.integration.transaction;

import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.enums.CategoryType;
import com.trako.enums.TransactionDbType;
import com.trako.integration.BaseIntegrationTest;
import com.trako.repositories.TransactionSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TransactionSearchRepository.
 * Validates: Requirements 1.1, 1.2, 3.1, 3.2, 3.3
 */
@Transactional
public class TransactionSearchRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private TransactionSearchRepository transactionSearchRepository;

    private User userA;
    private User userB;
    private Account accountA;
    private Account accountB;
    private Category categoryA;
    private Category categoryB;

    @BeforeEach
    public void setup() {
        // Create two distinct users for authorization tests
        userA = createUniqueUser("User A");
        userB = createUniqueUser("User B");

        // Accounts for each user
        accountA = new Account();
        accountA.setName("Account A");
        accountA.setUserId(userA.getId());
        accountA.setCurrency("INR");
        accountA = accountRepository.save(accountA);

        accountB = new Account();
        accountB.setName("Account B");
        accountB.setUserId(userB.getId());
        accountB.setCurrency("INR");
        accountB = accountRepository.save(accountB);

        // Categories for each user
        categoryA = new Category();
        categoryA.setName("Food");
        categoryA.setUserId(userA.getId());
        categoryA.setCategoryType(CategoryType.EXPENSE);
        categoryA = categoryRepository.save(categoryA);

        categoryB = new Category();
        categoryB.setName("Travel");
        categoryB.setUserId(userB.getId());
        categoryB.setCategoryType(CategoryType.EXPENSE);
        categoryB = categoryRepository.save(categoryB);
    }

    // ---- Authorization tests (Requirement 1.1) ----

    @Test
    public void searchReturnsOnlyAuthenticatedUsersTransactions() {
        createTransaction(accountA, categoryA, "Grocery Shopping", "Weekly groceries", date(2025, 6, 1), 50.0);
        createTransaction(accountB, categoryB, "Grocery Run", "Monthly groceries", date(2025, 6, 2), 75.0);

        Page<Transaction> resultsA = transactionSearchRepository.searchTransactions(
                userA.getId(), List.of("grocery"), null, null, null, null, null, null,
                PageRequest.of(0, 10));

        // User A should only see their own transaction
        assertEquals(1, resultsA.getTotalElements());
        assertEquals("Grocery Shopping", resultsA.getContent().get(0).getName());

        Page<Transaction> resultsB = transactionSearchRepository.searchTransactions(
                userB.getId(), List.of("grocery"), null, null, null, null, null, null,
                PageRequest.of(0, 10));

        // User B should only see their own transaction
        assertEquals(1, resultsB.getTotalElements());
        assertEquals("Grocery Run", resultsB.getContent().get(0).getName());
    }

    // ---- LIKE query on name (Requirement 1.2) ----

    @Test
    public void searchMatchesTransactionName() {
        createTransaction(accountA, categoryA, "Coffee at Starbucks", null, date(2025, 5, 10), 5.0);
        createTransaction(accountA, categoryA, "Lunch at Subway", null, date(2025, 5, 11), 12.0);

        Page<Transaction> results = transactionSearchRepository.searchTransactions(
                userA.getId(), List.of("coffee"), null, null, null, null, null, null,
                PageRequest.of(0, 10));

        assertEquals(1, results.getTotalElements());
        assertEquals("Coffee at Starbucks", results.getContent().get(0).getName());
    }

    // ---- LIKE query on comments (Requirement 1.2) ----

    @Test
    public void searchMatchesTransactionComments() {
        createTransaction(accountA, categoryA, "Dinner", "Birthday celebration at Italian place", date(2025, 4, 20), 80.0);
        createTransaction(accountA, categoryA, "Lunch", "Quick meal", date(2025, 4, 21), 15.0);

        Page<Transaction> results = transactionSearchRepository.searchTransactions(
                userA.getId(), List.of("birthday"), null, null, null, null, null, null,
                PageRequest.of(0, 10));

        assertEquals(1, results.getTotalElements());
        assertEquals("Dinner", results.getContent().get(0).getName());
    }

    // ---- Date range filtering (Requirements 3.1, 3.2, 3.3) ----

    @Test
    public void searchFiltersWithDateRange() {
        createTransaction(accountA, categoryA, "January Rent", null, date(2025, 1, 5), 1000.0);
        createTransaction(accountA, categoryA, "March Rent", null, date(2025, 3, 5), 1000.0);
        createTransaction(accountA, categoryA, "June Rent", null, date(2025, 6, 5), 1000.0);

        // Search for "rent" within Feb-Apr range (startDate inclusive, endDate exclusive)
        Page<Transaction> results = transactionSearchRepository.searchTransactions(
                userA.getId(), List.of("rent"), date(2025, 2, 1), date(2025, 5, 1),
                null, null, null, null,
                PageRequest.of(0, 10));

        assertEquals(1, results.getTotalElements());
        assertEquals("March Rent", results.getContent().get(0).getName());
    }

    // ---- Amount range filtering (Requirement 3.3) ----

    @Test
    public void searchFiltersWithAmountRange() {
        // amount is a generated column = originalAmount * exchangeRate
        // With exchangeRate=1.0, amount equals originalAmount
        createTransaction(accountA, categoryA, "Small Purchase", null, date(2025, 7, 1), 10.0);
        createTransaction(accountA, categoryA, "Medium Purchase", null, date(2025, 7, 2), 50.0);
        createTransaction(accountA, categoryA, "Large Purchase", null, date(2025, 7, 3), 200.0);

        Page<Transaction> results = transactionSearchRepository.searchTransactions(
                userA.getId(), List.of("purchase"), null, null, 20.0, 100.0, null, null,
                PageRequest.of(0, 10));

        assertEquals(1, results.getTotalElements());
        assertEquals("Medium Purchase", results.getContent().get(0).getName());
    }

    // ---- Pagination (Requirement 4.4) ----

    @Test
    public void searchPaginatesCorrectly() {
        // Create 5 transactions
        for (int i = 1; i <= 5; i++) {
            createTransaction(accountA, categoryA, "Item " + i, null, date(2025, 8, i), 10.0 * i);
        }

        // Page 0, size 2
        Page<Transaction> page0 = transactionSearchRepository.searchTransactions(
                userA.getId(), List.of("item"), null, null, null, null, null, null,
                PageRequest.of(0, 2));

        assertEquals(5, page0.getTotalElements());
        assertEquals(2, page0.getContent().size());
        assertEquals(3, page0.getTotalPages());
        assertTrue(page0.hasNext());
        assertFalse(page0.hasPrevious());

        // Page 1, size 2
        Page<Transaction> page1 = transactionSearchRepository.searchTransactions(
                userA.getId(), List.of("item"), null, null, null, null, null, null,
                PageRequest.of(1, 2));

        assertEquals(5, page1.getTotalElements());
        assertEquals(2, page1.getContent().size());
        assertTrue(page1.hasNext());
        assertTrue(page1.hasPrevious());

        // Page 2 (last page), size 2
        Page<Transaction> page2 = transactionSearchRepository.searchTransactions(
                userA.getId(), List.of("item"), null, null, null, null, null, null,
                PageRequest.of(2, 2));

        assertEquals(5, page2.getTotalElements());
        assertEquals(1, page2.getContent().size());
        assertFalse(page2.hasNext());
        assertTrue(page2.hasPrevious());
    }

    // ---- Helpers ----

    private Date date(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, 12, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Transaction createTransaction(Account account, Category category,
                                           String name, String comments,
                                           Date date, double originalAmount) {
        Transaction t = new Transaction();
        t.setName(name);
        t.setComments(comments);
        t.setDate(date);
        t.setOriginalAmount(originalAmount);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setAccountId(account.getId());
        t.setCategoryId(category.getId());
        t.setTransactionType(TransactionDbType.DEBIT);
        return transactionRepository.save(t);
    }
}
