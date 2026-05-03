package com.trako.integration.transaction;

import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.enums.CategoryType;
import com.trako.enums.TransactionDbType;
import com.trako.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Consolidated integration tests for GET /api/transactions/search.
 * Covers: input validation, authorization, search matching, filtering, and pagination.
 */
@Transactional
public class TransactionSearchIT extends BaseIntegrationTest {

    private User user;
    private String token;
    private Account account;
    private Category category;

    @BeforeEach
    void setup() {
        user = createUniqueUser();
        token = generateBearerToken(user);

        account = new Account();
        account.setName("Test Account");
        account.setUserId(user.getId());
        account.setCurrency("INR");
        account = accountRepository.save(account);

        category = new Category();
        category.setName("Food");
        category.setUserId(user.getId());
        category.setCategoryType(CategoryType.EXPENSE);
        category = categoryRepository.save(category);
    }

    // ---- Authorization ----

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "coffee"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchReturnsOnlyAuthenticatedUsersTransactions() throws Exception {
        createTx("Shared Grocery", null, new Date(), 50.0);

        User otherUser = createUniqueUser();
        String otherToken = generateBearerToken(otherUser);
        Account otherAccount = new Account();
        otherAccount.setName("Other Account");
        otherAccount.setUserId(otherUser.getId());
        otherAccount.setCurrency("INR");
        otherAccount = accountRepository.save(otherAccount);
        Category otherCategory = new Category();
        otherCategory.setName("Other");
        otherCategory.setUserId(otherUser.getId());
        otherCategory.setCategoryType(CategoryType.EXPENSE);
        otherCategory = categoryRepository.save(otherCategory);
        Transaction otherTx = new Transaction();
        otherTx.setName("Shared Grocery");
        otherTx.setDate(new Date());
        otherTx.setOriginalAmount(75.0);
        otherTx.setOriginalCurrency("INR");
        otherTx.setExchangeRate(1.0);
        otherTx.setAccountId(otherAccount.getId());
        otherTx.setCategoryId(otherCategory.getId());
        otherTx.setTransactionType(TransactionDbType.DEBIT);
        transactionRepository.save(otherTx);

        // Each user should only see their own transaction
        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "grocery")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(1));

        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "grocery")
                        .header("Authorization", otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(1));
    }

    // ---- Input Validation ----

    @Test
    void emptyQuery_returns400() throws Exception {
        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void queryTooLong_returns400() throws Exception {
        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "a".repeat(201))
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidDateRange_returns400() throws Exception {
        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "coffee")
                        .param("startDate", "2024-12-31")
                        .param("endDate", "2024-01-01")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    // ---- Search Matching ----

    @Test
    void successfulSearch_returns200WithResults() throws Exception {
        createTx("Coffee Shop", null, new Date(), 5.0);

        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "coffee")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.result.results").isArray())
                .andExpect(jsonPath("$.result.query").value("coffee"));
    }

    @Test
    void nonexistentQuery_returnsEmptyResult() throws Exception {
        createTx("coffee", null, new Date(), 5.0);

        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "nonexistent")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(0))
                .andExpect(jsonPath("$.result.results").isEmpty());
    }

    @Test
    void mixedCaseQuery_matchesCaseInsensitively() throws Exception {
        createTx("Coffee Shop", null, new Date(), 5.0);

        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "COFFEE")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(1));
    }

    @Test
    void extraWhitespace_stillMatches() throws Exception {
        createTx("Coffee Shop", null, new Date(), 5.0);

        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "  coffee   shop  ")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(1));
    }

    @Test
    void searchMatchesComments() throws Exception {
        createTx("Dinner", "Birthday celebration", new Date(), 80.0);

        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "birthday")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(1));
    }

    // ---- Filtering ----

    @Test
    void dateRangeFilter_excludesOutsideRange() throws Exception {
        createTx("Rent Jan", null, date(2025, 1, 5), 1000.0);
        createTx("Rent Mar", null, date(2025, 3, 5), 1000.0);
        createTx("Rent Jun", null, date(2025, 6, 5), 1000.0);

        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "rent")
                        .param("startDate", "2025-02-01")
                        .param("endDate", "2025-05-01")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(1));
    }

    @Test
    void amountRangeFilter_excludesOutsideRange() throws Exception {
        createTx("Small Purchase", null, new Date(), 10.0);
        createTx("Medium Purchase", null, new Date(), 50.0);
        createTx("Large Purchase", null, new Date(), 200.0);

        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "purchase")
                        .param("minAmount", "20")
                        .param("maxAmount", "100")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(1));
    }

    // ---- Pagination ----

    @Test
    void pagination_firstPage() throws Exception {
        for (int i = 1; i <= 25; i++) createTx("item " + i, null, new Date(), 10.0);

        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "item")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(25))
                .andExpect(jsonPath("$.result.results", hasSize(10)))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(10))
                .andExpect(jsonPath("$.result.totalPages").value(3))
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andExpect(jsonPath("$.result.hasPrevious").value(false));
    }

    @Test
    void pagination_middlePage() throws Exception {
        for (int i = 1; i <= 25; i++) createTx("item " + i, null, new Date(), 10.0);

        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "item")
                        .param("page", "1")
                        .param("size", "10")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andExpect(jsonPath("$.result.hasPrevious").value(true));
    }

    @Test
    void pagination_lastPage() throws Exception {
        for (int i = 1; i <= 25; i++) createTx("item " + i, null, new Date(), 10.0);

        mockMvc.perform(get("/api/transactions/search")
                        .param("query", "item")
                        .param("page", "2")
                        .param("size", "10")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.results", hasSize(5)))
                .andExpect(jsonPath("$.result.hasNext").value(false))
                .andExpect(jsonPath("$.result.hasPrevious").value(true));
    }

    // ---- Helpers ----

    private Date date(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, 12, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Transaction createTx(String name, String comments, Date date, double amount) {
        Transaction t = new Transaction();
        t.setName(name);
        t.setComments(comments);
        t.setDate(date);
        t.setOriginalAmount(amount);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setAccountId(account.getId());
        t.setCategoryId(category.getId());
        t.setTransactionType(TransactionDbType.DEBIT);
        return transactionRepository.save(t);
    }
}
