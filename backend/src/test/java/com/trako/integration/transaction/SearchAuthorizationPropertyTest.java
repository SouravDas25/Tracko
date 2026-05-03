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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based test for Search Authorization Boundary.
 * Validates: Requirements 1.1, 1.2
 */
@Transactional
public class SearchAuthorizationPropertyTest extends BaseIntegrationTest {

    private static final Random RANDOM = new Random();
    private static final String[] SHARED_TERMS = {
            "grocery", "coffee", "rent", "dinner", "shopping",
            "travel", "medical", "insurance", "utility", "subscription"
    };

    private String tokenA;
    private String tokenB;
    private Set<Long> userAAccountIds;
    private Set<Long> userBAccountIds;

    @BeforeEach
    public void setup() {
        User userA = createUniqueUser("Auth Property User A");
        User userB = createUniqueUser("Auth Property User B");
        tokenA = generateBearerToken(userA);
        tokenB = generateBearerToken(userB);

        List<Account> accsA = createAccounts(userA, 2);
        List<Account> accsB = createAccounts(userB, 2);
        userAAccountIds = accsA.stream().map(Account::getId).collect(Collectors.toSet());
        userBAccountIds = accsB.stream().map(Account::getId).collect(Collectors.toSet());

        Category catA = createCategory(userA, "General A");
        Category catB = createCategory(userB, "General B");
        populateTransactions(accsA, catA, 10);
        populateTransactions(accsB, catB, 10);
    }

    @RepeatedTest(5)
    public void searchResultsContainOnlySearchingUsersTransactions() throws Exception {
        String term = SHARED_TERMS[RANDOM.nextInt(SHARED_TERMS.length)];

        String respA = mockMvc.perform(get("/api/transactions/search")
                        .param("query", term).param("size", "100")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Integer> idsA = JsonPath.read(respA, "$.result.results[*].transaction.accountId");
        for (Integer id : idsA) {
            assertTrue(userAAccountIds.contains(id.longValue()),
                    "User A search returned account " + id + " not owned by User A");
            assertFalse(userBAccountIds.contains(id.longValue()),
                    "User A search returned account belonging to User B");
        }

        String respB = mockMvc.perform(get("/api/transactions/search")
                        .param("query", term).param("size", "100")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Integer> idsB = JsonPath.read(respB, "$.result.results[*].transaction.accountId");
        for (Integer id : idsB) {
            assertTrue(userBAccountIds.contains(id.longValue()),
                    "User B search returned account " + id + " not owned by User B");
            assertFalse(userAAccountIds.contains(id.longValue()),
                    "User B search returned account belonging to User A");
        }
    }

    @RepeatedTest(3)
    public void filteringByOtherUsersAccountIdsReturnsNothing() throws Exception {
        String accountIds = userBAccountIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        String resp = mockMvc.perform(get("/api/transactions/search")
                        .param("query", "payment").param("accountIds", accountIds).param("size", "100")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(0, (int) JsonPath.read(resp, "$.result.totalResults"));
    }

    @RepeatedTest(3)
    public void emptyQueryRespectsAuthorizationBoundary() throws Exception {
        String respA = mockMvc.perform(get("/api/transactions/search")
                        .param("query", "payment").param("size", "100")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Integer> idsA = JsonPath.read(respA, "$.result.results[*].transaction.accountId");
        for (Integer id : idsA) {
            assertTrue(userAAccountIds.contains(id.longValue()),
                    "User A search returned account " + id + " not owned by User A");
            assertFalse(userBAccountIds.contains(id.longValue()),
                    "User A search returned account belonging to User B");
        }

        String respB = mockMvc.perform(get("/api/transactions/search")
                        .param("query", "payment").param("size", "100")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Integer> idsB = JsonPath.read(respB, "$.result.results[*].transaction.accountId");
        for (Integer id : idsB) {
            assertTrue(userBAccountIds.contains(id.longValue()),
                    "User B search returned account " + id + " not owned by User B");
            assertFalse(userAAccountIds.contains(id.longValue()),
                    "User B search returned account belonging to User A");
        }
    }

    @RepeatedTest(3)
    public void combinedFiltersRespectAuthorizationBoundary() throws Exception {
        String term = SHARED_TERMS[RANDOM.nextInt(SHARED_TERMS.length)];

        String respA = mockMvc.perform(get("/api/transactions/search")
                        .param("query", term)
                        .param("startDate", "2024-01-01").param("endDate", "2025-12-31")
                        .param("minAmount", "10").param("maxAmount", "300")
                        .param("size", "100")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Integer> idsA = JsonPath.read(respA, "$.result.results[*].transaction.accountId");
        for (Integer id : idsA) {
            assertTrue(userAAccountIds.contains(id.longValue()),
                    "Combined-filter returned account " + id + " not owned by User A");
        }

        String respB = mockMvc.perform(get("/api/transactions/search")
                        .param("query", term)
                        .param("startDate", "2024-01-01").param("endDate", "2025-12-31")
                        .param("minAmount", "10").param("maxAmount", "300")
                        .param("size", "100")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Integer> idsB = JsonPath.read(respB, "$.result.results[*].transaction.accountId");
        for (Integer id : idsB) {
            assertTrue(userBAccountIds.contains(id.longValue()),
                    "Combined-filter returned account " + id + " not owned by User B");
        }
    }

    private List<Account> createAccounts(User user, int count) {
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Account a = new Account();
            a.setName(user.getName() + " Account " + (i + 1));
            a.setUserId(user.getId());
            a.setCurrency("INR");
            accounts.add(accountRepository.save(a));
        }
        return accounts;
    }

    private Category createCategory(User user, String name) {
        Category c = new Category();
        c.setName(name);
        c.setUserId(user.getId());
        c.setCategoryType(CategoryType.EXPENSE);
        return categoryRepository.save(c);
    }

    private void populateTransactions(List<Account> accounts, Category category, int count) {
        for (int i = 0; i < count; i++) {
            Account acc = accounts.get(RANDOM.nextInt(accounts.size()));
            Transaction t = new Transaction();
            t.setName(SHARED_TERMS[RANDOM.nextInt(SHARED_TERMS.length)] + " payment " + UUID.randomUUID().toString().substring(0, 6));
            t.setDate(randomDate());
            t.setOriginalAmount(10.0 + RANDOM.nextDouble() * 200.0);
            t.setOriginalCurrency("INR");
            t.setExchangeRate(1.0);
            t.setAccountId(acc.getId());
            t.setCategoryId(category.getId());
            t.setTransactionType(TransactionDbType.DEBIT);
            transactionRepository.save(t);
        }
    }

    private Date randomDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024 + RANDOM.nextInt(2), RANDOM.nextInt(12), 1 + RANDOM.nextInt(28), 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
