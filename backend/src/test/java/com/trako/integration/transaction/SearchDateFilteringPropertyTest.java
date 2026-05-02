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
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for Historical Scope with Optional Date Filtering.
 *
 * <p><b>Property 4: Historical Scope with Optional Date Filtering</b></p>
 * For any transaction, verify it's searchable when no date range is specified;
 * when a date range is specified, only transactions within that range are returned.
 *
 * <p><b>Validates: Requirements 3.1, 3.2, 3.3</b></p>
 *
 * <p>Uses repeated tests with randomized data to simulate property-based testing
 * behavior across multiple iterations. Transactions are created across years
 * 2020-2025 with a shared search term, then verified with and without date filters.</p>
 */
@Transactional
public class SearchDateFilteringPropertyTest extends BaseIntegrationTest {

    @Autowired
    private TransactionSearchRepository transactionSearchRepository;

    private static final Random RANDOM = new Random();

    /** A unique search term embedded in all test transactions. */
    private static final String SHARED_SEARCH_TERM = "historicalprop";

    /** Years across which transactions are distributed. */
    private static final int[] YEARS = {2020, 2021, 2022, 2023, 2024, 2025};

    private User user;
    private Account account;
    private Category category;

    /** Tracks which year each transaction was created in, keyed by transaction ID. */
    private final Map<Long, Integer> transactionYearMap = new HashMap<>();

    /** All transaction IDs created during setup. */
    private final Set<Long> allTransactionIds = new HashSet<>();

    @BeforeEach
    public void setup() {
        user = createUniqueUser("Date Filter Property User");

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

        // Create 2 transactions per year across 2020-2025
        for (int year : YEARS) {
            for (int i = 0; i < 2; i++) {
                Transaction t = new Transaction();
                t.setName(SHARED_SEARCH_TERM + " payment " + year + "-" + i);
                t.setComments("Comment for year " + year);
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

    /**
     * Property: when no date range is specified, transactions from all years are searchable.
     * Repeated 5 times to cover randomized transaction dates within each year.
     */
    @RepeatedTest(5)
    public void noDateRangeReturnsTransactionsFromAllYears() {
        Page<Transaction> results = transactionSearchRepository.searchTransactions(
                user.getId(),
                List.of(SHARED_SEARCH_TERM),
                null, // no startDate
                null, // no endDate
                null, null, null, null,
                PageRequest.of(0, 100));

        // All transactions with the shared term should be returned
        Set<Long> returnedIds = new HashSet<>();
        for (Transaction tx : results.getContent()) {
            returnedIds.add(tx.getId());
        }

        assertEquals(allTransactionIds.size(), returnedIds.size(),
                "Without date filter, all " + allTransactionIds.size()
                        + " transactions should be returned but got " + returnedIds.size());

        for (Long expectedId : allTransactionIds) {
            assertTrue(returnedIds.contains(expectedId),
                    "Transaction id=" + expectedId + " (year "
                            + transactionYearMap.get(expectedId)
                            + ") should be searchable without date filter");
        }

        // Verify transactions span multiple years
        Set<Integer> yearsFound = new HashSet<>();
        for (Transaction tx : results.getContent()) {
            Integer year = transactionYearMap.get(tx.getId());
            if (year != null) {
                yearsFound.add(year);
            }
        }
        assertTrue(yearsFound.size() > 1,
                "Results should span multiple years; found years: " + yearsFound);
    }

    /**
     * Property: when a date range is specified, only transactions within that range are returned.
     * Picks a random contiguous subset of years and verifies only those transactions appear.
     */
    @RepeatedTest(5)
    public void dateRangeReturnsOnlyTransactionsWithinRange() {
        // Pick a random start and end year from the available years
        int startIdx = RANDOM.nextInt(YEARS.length - 1);
        int endIdx = startIdx + 1 + RANDOM.nextInt(YEARS.length - startIdx - 1);
        int startYear = YEARS[startIdx];
        int endYear = YEARS[endIdx];

        Calendar cal = Calendar.getInstance();

        // startDate: January 1 of startYear (inclusive)
        cal.set(startYear, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startDate = cal.getTime();

        // endDate: January 1 of endYear+1 (exclusive, per repository implementation)
        cal.set(endYear + 1, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date endDate = cal.getTime();

        Page<Transaction> results = transactionSearchRepository.searchTransactions(
                user.getId(),
                List.of(SHARED_SEARCH_TERM),
                startDate,
                endDate,
                null, null, null, null,
                PageRequest.of(0, 100));

        // Build expected set: transactions whose year falls within [startYear, endYear]
        Set<Long> expectedIds = new HashSet<>();
        for (Map.Entry<Long, Integer> entry : transactionYearMap.entrySet()) {
            int txYear = entry.getValue();
            if (txYear >= startYear && txYear <= endYear) {
                expectedIds.add(entry.getKey());
            }
        }

        Set<Long> returnedIds = new HashSet<>();
        for (Transaction tx : results.getContent()) {
            returnedIds.add(tx.getId());
        }

        // Every returned transaction must be within the date range
        for (Transaction tx : results.getContent()) {
            Integer txYear = transactionYearMap.get(tx.getId());
            assertNotNull(txYear,
                    "Returned transaction id=" + tx.getId() + " was not created in setup");
            assertTrue(txYear >= startYear && txYear <= endYear,
                    "Transaction id=" + tx.getId() + " (year " + txYear
                            + ") is outside the requested range [" + startYear + ", " + endYear + "]");
            // Also verify the actual date is within bounds
            assertFalse(tx.getDate().before(startDate),
                    "Transaction date " + tx.getDate() + " is before startDate " + startDate);
            assertTrue(tx.getDate().before(endDate),
                    "Transaction date " + tx.getDate() + " is not before endDate " + endDate);
        }

        // All expected transactions should be present
        for (Long expectedId : expectedIds) {
            assertTrue(returnedIds.contains(expectedId),
                    "Transaction id=" + expectedId + " (year "
                            + transactionYearMap.get(expectedId)
                            + ") should be within range [" + startYear + ", " + endYear
                            + "] but was not returned");
        }
    }

    /**
     * Property: transactions outside the specified date range are excluded.
     * Uses a narrow single-year window and verifies transactions from other years are absent.
     */
    @RepeatedTest(5)
    public void transactionsOutsideDateRangeAreExcluded() {
        // Pick a single random year as the filter window
        int targetYear = YEARS[RANDOM.nextInt(YEARS.length)];

        Calendar cal = Calendar.getInstance();

        cal.set(targetYear, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startDate = cal.getTime();

        cal.set(targetYear + 1, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date endDate = cal.getTime();

        Page<Transaction> results = transactionSearchRepository.searchTransactions(
                user.getId(),
                List.of(SHARED_SEARCH_TERM),
                startDate,
                endDate,
                null, null, null, null,
                PageRequest.of(0, 100));

        // Count how many transactions we expect for the target year
        long expectedCount = transactionYearMap.values().stream()
                .filter(y -> y == targetYear)
                .count();

        assertEquals(expectedCount, results.getTotalElements(),
                "For year " + targetYear + ", expected " + expectedCount
                        + " transactions but got " + results.getTotalElements());

        // No transaction from a different year should appear
        for (Transaction tx : results.getContent()) {
            Integer txYear = transactionYearMap.get(tx.getId());
            assertNotNull(txYear,
                    "Returned transaction id=" + tx.getId() + " was not created in setup");
            assertEquals(targetYear, txYear.intValue(),
                    "Transaction id=" + tx.getId() + " (year " + txYear
                            + ") should not appear in results for year " + targetYear);
        }

        // Verify that transactions from other years are NOT in the results
        Set<Long> returnedIds = new HashSet<>();
        for (Transaction tx : results.getContent()) {
            returnedIds.add(tx.getId());
        }

        for (Map.Entry<Long, Integer> entry : transactionYearMap.entrySet()) {
            if (entry.getValue() != targetYear) {
                assertFalse(returnedIds.contains(entry.getKey()),
                        "Transaction id=" + entry.getKey() + " (year " + entry.getValue()
                                + ") should be excluded when filtering for year " + targetYear);
            }
        }
    }

    // ---- Helper methods ----

    /**
     * Generate a random date within the given year.
     */
    private Date randomDateInYear(int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, RANDOM.nextInt(12), 1 + RANDOM.nextInt(28), 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
