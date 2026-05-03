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
 * Property-based test for Historical Scope with Optional Date Filtering.
 * Validates: Requirements 3.1, 3.2, 3.3
 */
@Transactional
public class SearchDateFilteringPropertyTest extends BaseIntegrationTest {

    private static final Random RANDOM = new Random();
    private static final String SHARED_SEARCH_TERM = "historicalprop";
    private static final int[] YEARS = {2020, 2021, 2022, 2023, 2024, 2025};

    private String token;
    private Account account;
    private Category category;
    private final Map<Long, Integer> transactionYearMap = new HashMap<>();
    private final Set<Long> allTransactionIds = new HashSet<>();

    @BeforeEach
    public void setup() {
        User user = createUniqueUser("Date Filter Property User");
        token = generateBearerToken(user);

        account = new Account();
        account.setName("Date Filter Account");
        account.setUserId(user.getId());
        account.setCurrency("INR");
        account = accountRepository.save(account);

        category = new Category();
        category.setName("Date Filter Category");
        category.setUserId(user.getId());
        category.setCategoryType(CategoryType.EXPENSE);
        category = categoryRepository.save(category);

        transactionYearMap.clear();
        allTransactionIds.clear();

        for (int year : YEARS) {
            for (int i = 0; i < 2; i++) {
                Transaction t = new Transaction();
                t.setName(SHARED_SEARCH_TERM + " payment " + year + "-" + i);
                t.setDate(randomDateInYear(year));
                t.setOriginalAmount(10.0 + RANDOM.nextDouble() * 200.0);
                t.setOriginalCurrency("INR");
                t.setExchangeRate(1.0);
                t.setAccountId(account.getId());
                t.setCategoryId(category.getId());
                t.setTransactionType(TransactionDbType.DEBIT);
                Transaction saved = transactionRepository.save(t);
                transactionYearMap.put(saved.getId(), year);
                allTransactionIds.add(saved.getId());
            }
        }
    }

    @RepeatedTest(5)
    public void noDateRangeReturnsTransactionsFromAllYears() throws Exception {
        String response = mockMvc.perform(get("/api/transactions/search")
                        .param("query", SHARED_SEARCH_TERM)
                        .param("size", "100")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Integer totalResults = JsonPath.read(response, "$.result.totalResults");
        assertEquals(allTransactionIds.size(), totalResults,
                "Without date filter, all transactions should be returned");

        List<Integer> returnedIds = JsonPath.read(response, "$.result.results[*].transaction.id");
        Set<Long> returnedIdSet = new HashSet<>();
        for (Integer id : returnedIds) returnedIdSet.add(id.longValue());

        for (Long expectedId : allTransactionIds) {
            assertTrue(returnedIdSet.contains(expectedId),
                    "Transaction id=" + expectedId + " (year " + transactionYearMap.get(expectedId)
                            + ") should be searchable without date filter");
        }

        Set<Integer> yearsFound = new HashSet<>();
        for (Integer id : returnedIds) {
            Integer year = transactionYearMap.get(id.longValue());
            if (year != null) yearsFound.add(year);
        }
        assertTrue(yearsFound.size() > 1, "Results should span multiple years; found: " + yearsFound);
    }

    @RepeatedTest(5)
    public void dateRangeReturnsOnlyTransactionsWithinRange() throws Exception {
        int startIdx = RANDOM.nextInt(YEARS.length - 1);
        int endIdx = startIdx + 1 + RANDOM.nextInt(YEARS.length - startIdx - 1);
        int startYear = YEARS[startIdx];
        int endYear = YEARS[endIdx];

        String startDate = startYear + "-01-01";
        String endDate = (endYear + 1) + "-01-01";

        String response = mockMvc.perform(get("/api/transactions/search")
                        .param("query", SHARED_SEARCH_TERM)
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .param("size", "100")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Integer> returnedIds = JsonPath.read(response, "$.result.results[*].transaction.id");

        long expectedCount = transactionYearMap.values().stream()
                .filter(y -> y >= startYear && y <= endYear).count();
        assertEquals(expectedCount, returnedIds.size(),
                "Expected " + expectedCount + " transactions for range [" + startYear + ", " + endYear + "]");

        for (Integer id : returnedIds) {
            Integer txYear = transactionYearMap.get(id.longValue());
            assertNotNull(txYear, "Returned transaction id=" + id + " was not created in setup");
            assertTrue(txYear >= startYear && txYear <= endYear,
                    "Transaction id=" + id + " (year " + txYear + ") is outside range [" + startYear + ", " + endYear + "]");
        }
    }

    @RepeatedTest(5)
    public void transactionsOutsideDateRangeAreExcluded() throws Exception {
        int targetYear = YEARS[RANDOM.nextInt(YEARS.length)];

        String response = mockMvc.perform(get("/api/transactions/search")
                        .param("query", SHARED_SEARCH_TERM)
                        .param("startDate", targetYear + "-01-01")
                        .param("endDate", (targetYear + 1) + "-01-01")
                        .param("size", "100")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long expectedCount = transactionYearMap.values().stream()
                .filter(y -> y == targetYear).count();
        Integer totalResults = JsonPath.read(response, "$.result.totalResults");
        assertEquals(expectedCount, totalResults.longValue(),
                "For year " + targetYear + ", expected " + expectedCount + " transactions");

        List<Integer> returnedIds = JsonPath.read(response, "$.result.results[*].transaction.id");
        Set<Long> returnedIdSet = new HashSet<>();
        for (Integer id : returnedIds) returnedIdSet.add(id.longValue());

        for (Map.Entry<Long, Integer> entry : transactionYearMap.entrySet()) {
            if (entry.getValue() != targetYear) {
                assertFalse(returnedIdSet.contains(entry.getKey()),
                        "Transaction id=" + entry.getKey() + " (year " + entry.getValue()
                                + ") should be excluded when filtering for year " + targetYear);
            }
        }
    }

    private Date randomDateInYear(int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, RANDOM.nextInt(12), 1 + RANDOM.nextInt(28), 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
