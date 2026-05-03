package com.trako.integration.transaction;

import com.jayway.jsonpath.JsonPath;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.enums.CategoryType;
import com.trako.enums.TransactionDbType;
import com.trako.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based test for Pagination Completeness.
 * Validates: Requirements 4.4
 */
@Transactional
public class SearchPaginationPropertyTest extends BaseIntegrationTest {

    private static final Random RANDOM = new Random();
    private static final String SEARCH_TERM_PREFIX = "paginationtest";

    private String token;
    private Account account;
    private Category category;

    @BeforeEach
    public void setup() {
        User user = createUniqueUser("Pagination Property User");
        token = generateBearerToken(user);

        account = new Account();
        account.setName("Pagination Test Account");
        account.setUserId(user.getId());
        account.setCurrency("INR");
        account = accountRepository.save(account);

        category = new Category();
        category.setName("Pagination Category");
        category.setUserId(user.getId());
        category.setCategoryType(CategoryType.EXPENSE);
        category = categoryRepository.save(category);
    }

    @RepeatedTest(5)
    public void allPagesContainExactlyNDistinctTransactions() throws Exception {
        int total = 7, pageSize = 3;
        String uniqueTerm = SEARCH_TERM_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        Set<Long> createdIds = createTransactions(uniqueTerm, total);

        Set<Long> collectedIds = new HashSet<>();
        int currentPage = 0;
        boolean hasMore = true;

        while (hasMore) {
            String response = mockMvc.perform(get("/api/transactions/search")
                            .param("query", uniqueTerm)
                            .param("page", String.valueOf(currentPage))
                            .param("size", String.valueOf(pageSize))
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            List<Integer> ids = JsonPath.read(response, "$.result.results[*].transaction.id");
            assertTrue(ids.size() <= pageSize);
            for (Integer id : ids) {
                assertTrue(collectedIds.add(id.longValue()), "Duplicate ID " + id + " on page " + currentPage);
            }
            hasMore = JsonPath.read(response, "$.result.hasNext");
            currentPage++;
            if (currentPage > total) fail("Too many pages");
        }

        assertEquals(createdIds.size(), collectedIds.size());
        assertEquals((int) Math.ceil((double) total / pageSize), currentPage);
    }

    @RepeatedTest(3)
    public void singlePageContainsAllResultsWhenSizeEqualsTotal() throws Exception {
        int total = 5;
        String uniqueTerm = SEARCH_TERM_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        createTransactions(uniqueTerm, total);

        String response = mockMvc.perform(get("/api/transactions/search")
                        .param("query", uniqueTerm)
                        .param("page", "0")
                        .param("size", String.valueOf(total))
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Integer> ids = JsonPath.read(response, "$.result.results[*].transaction.id");
        assertEquals(total, ids.size());
        assertFalse((Boolean) JsonPath.read(response, "$.result.hasNext"));
    }

    @RepeatedTest(3)
    public void pageSizeOneReturnsAllTransactionsAcrossNPages() throws Exception {
        int total = 4;
        String uniqueTerm = SEARCH_TERM_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        Set<Long> createdIds = createTransactions(uniqueTerm, total);

        Set<Long> collectedIds = new HashSet<>();
        int currentPage = 0;
        boolean hasMore = true;

        while (hasMore) {
            String response = mockMvc.perform(get("/api/transactions/search")
                            .param("query", uniqueTerm)
                            .param("page", String.valueOf(currentPage))
                            .param("size", "1")
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            List<Integer> ids = JsonPath.read(response, "$.result.results[*].transaction.id");
            for (Integer id : ids) {
                assertTrue(collectedIds.add(id.longValue()), "Duplicate ID " + id);
            }
            hasMore = JsonPath.read(response, "$.result.hasNext");
            currentPage++;
            if (currentPage > total + 1) fail("Too many pages");
        }

        assertEquals(createdIds, collectedIds);
        assertEquals(total, currentPage);
    }

    private Set<Long> createTransactions(String searchTerm, int count) {
        Set<Long> ids = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            Transaction t = new Transaction();
            t.setName(searchTerm + " item " + (i + 1));
            t.setDate(randomDate());
            t.setOriginalAmount(10.0 + RANDOM.nextDouble() * 100.0);
            t.setOriginalCurrency("INR");
            t.setExchangeRate(1.0);
            t.setAccountId(account.getId());
            t.setCategoryId(category.getId());
            t.setTransactionType(TransactionDbType.DEBIT);
            ids.add(transactionRepository.save(t).getId());
        }
        transactionRepository.flush();
        return ids;
    }

    private Date randomDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, RANDOM.nextInt(12), 1 + RANDOM.nextInt(28), 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
