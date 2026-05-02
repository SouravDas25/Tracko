package com.trako.integration.transaction;

import com.trako.dtos.SearchRequestDTO;
import com.trako.dtos.TransactionSearchHitDTO;
import com.trako.dtos.TransactionSearchResultDTO;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.enums.CategoryType;
import com.trako.enums.TransactionDbType;
import com.trako.integration.BaseIntegrationTest;
import com.trako.services.TransactionSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for Pagination Completeness.
 *
 * <p><b>Property 8: Pagination Completeness</b></p>
 * For any search result set with N total results and page size P,
 * iterating through all pages returns exactly N distinct transactions
 * with no omissions or duplicates.
 *
 * <p><b>Validates: Requirements 4.4</b></p>
 *
 * <p>Uses repeated tests with randomized data to simulate property-based
 * testing behavior across multiple iterations.</p>
 */
@Transactional
public class SearchPaginationPropertyTest extends BaseIntegrationTest {

    @Autowired
    private TransactionSearchService transactionSearchService;

    private static final Random RANDOM = new Random();

    /** A unique search term used to isolate test transactions from other data. */
    private static final String SEARCH_TERM_PREFIX = "paginationtest";

    private User user;
    private Account account;
    private Category category;

    @BeforeEach
    public void setup() {
        user = createUniqueUser("Pagination Property User");

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

    /**
     * Property: iterating through all pages with a small page size collects
     * exactly N distinct transaction IDs with no duplicates or omissions.
     *
     * Creates 7 transactions with a shared unique search term, then paginates
     * with page size 3 and verifies completeness.
     */
    @RepeatedTest(5)
    public void allPagesContainExactlyNDistinctTransactions() {
        int totalTransactions = 7;
        int pageSize = 3;

        // Generate a unique search term per iteration to avoid cross-test interference
        String uniqueTerm = SEARCH_TERM_PREFIX + UUID.randomUUID().toString().substring(0, 8);

        // Create N transactions with the shared search term in the name
        Set<Long> createdIds = createTransactionsWithTerm(uniqueTerm, totalTransactions);
        assertEquals(totalTransactions, createdIds.size(),
                "Setup should create exactly " + totalTransactions + " transactions");

        // Iterate through all pages collecting transaction IDs
        Set<Long> collectedIds = new HashSet<>();
        int currentPage = 0;
        int totalPagesVisited = 0;
        boolean hasMore = true;

        while (hasMore) {
            SearchRequestDTO request = new SearchRequestDTO();
            request.setQuery(uniqueTerm);
            request.setPage(currentPage);
            request.setSize(pageSize);

            TransactionSearchResultDTO result = transactionSearchService.search(
                    user.getId(), request);

            assertNotNull(result, "Search result should not be null for page " + currentPage);
            assertNotNull(result.getResults(),
                    "Results list should not be null for page " + currentPage);

            // Verify: no page returns more than P results
            assertTrue(result.getResults().size() <= pageSize,
                    "Page " + currentPage + " returned " + result.getResults().size()
                            + " results, exceeding page size " + pageSize);

            // Collect IDs from this page
            for (TransactionSearchHitDTO hit : result.getResults()) {
                assertNotNull(hit.getTransaction(), "Hit transaction should not be null");
                assertNotNull(hit.getTransaction().getId(), "Transaction ID should not be null");
                collectedIds.add(hit.getTransaction().getId());
            }

            totalPagesVisited++;
            currentPage++;
            hasMore = result.getHasNext() != null && result.getHasNext();

            // Safety guard against infinite loops
            if (totalPagesVisited > totalTransactions) {
                fail("Visited more pages than total transactions — possible infinite loop. "
                        + "Pages visited: " + totalPagesVisited);
            }
        }

        // Verify: total distinct IDs == N (no duplicates, no omissions)
        assertEquals(createdIds.size(), collectedIds.size(),
                "Paginating through all pages should return exactly " + createdIds.size()
                        + " distinct transactions, but got " + collectedIds.size()
                        + ". Created IDs: " + createdIds + ", Collected IDs: " + collectedIds);

        // Verify: the collected IDs are exactly the created IDs
        assertEquals(createdIds, collectedIds,
                "The set of collected transaction IDs across all pages should match "
                        + "the set of created transaction IDs exactly");

        // Verify expected number of pages
        int expectedPages = (int) Math.ceil((double) totalTransactions / pageSize);
        assertEquals(expectedPages, totalPagesVisited,
                "Expected " + expectedPages + " pages for " + totalTransactions
                        + " transactions with page size " + pageSize);
    }

    /**
     * Property: pagination with page size equal to total results returns
     * everything in a single page with no next page.
     */
    @RepeatedTest(3)
    public void singlePageContainsAllResultsWhenSizeEqualsTotal() {
        int totalTransactions = 5;
        String uniqueTerm = SEARCH_TERM_PREFIX + UUID.randomUUID().toString().substring(0, 8);

        Set<Long> createdIds = createTransactionsWithTerm(uniqueTerm, totalTransactions);

        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery(uniqueTerm);
        request.setPage(0);
        request.setSize(totalTransactions);

        TransactionSearchResultDTO result = transactionSearchService.search(
                user.getId(), request);

        assertNotNull(result);
        assertEquals(totalTransactions, result.getResults().size(),
                "Single page with size=" + totalTransactions
                        + " should return all transactions");

        Set<Long> collectedIds = result.getResults().stream()
                .map(hit -> hit.getTransaction().getId())
                .collect(Collectors.toSet());

        assertEquals(createdIds, collectedIds,
                "Single-page result should contain exactly the created transaction IDs");

        assertFalse(result.getHasNext(),
                "There should be no next page when all results fit in one page");
    }

    /**
     * Property: pagination with page size of 1 still returns all N distinct
     * transactions across N pages.
     */
    @RepeatedTest(3)
    public void pageSizeOneReturnsAllTransactionsAcrossNPages() {
        int totalTransactions = 4;
        int pageSize = 1;
        String uniqueTerm = SEARCH_TERM_PREFIX + UUID.randomUUID().toString().substring(0, 8);

        Set<Long> createdIds = createTransactionsWithTerm(uniqueTerm, totalTransactions);

        Set<Long> collectedIds = new HashSet<>();
        int currentPage = 0;
        boolean hasMore = true;

        while (hasMore) {
            SearchRequestDTO request = new SearchRequestDTO();
            request.setQuery(uniqueTerm);
            request.setPage(currentPage);
            request.setSize(pageSize);

            TransactionSearchResultDTO result = transactionSearchService.search(
                    user.getId(), request);

            assertNotNull(result);
            assertTrue(result.getResults().size() <= pageSize,
                    "Page " + currentPage + " should return at most " + pageSize + " result");

            for (TransactionSearchHitDTO hit : result.getResults()) {
                boolean added = collectedIds.add(hit.getTransaction().getId());
                assertTrue(added,
                        "Duplicate transaction ID " + hit.getTransaction().getId()
                                + " found on page " + currentPage);
            }

            currentPage++;
            hasMore = result.getHasNext() != null && result.getHasNext();

            if (currentPage > totalTransactions + 1) {
                fail("Too many pages visited — possible infinite loop");
            }
        }

        assertEquals(createdIds, collectedIds,
                "Page size 1 pagination should collect all created transaction IDs");
        assertEquals(totalTransactions, currentPage,
                "Should visit exactly " + totalTransactions + " pages with page size 1");
    }

    // ---- Helper methods ----

    /**
     * Create N transactions whose name contains the given search term.
     * Returns the set of persisted transaction IDs.
     */
    private Set<Long> createTransactionsWithTerm(String searchTerm, int count) {
        Set<Long> ids = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            Transaction t = new Transaction();
            t.setName(searchTerm + " item " + (i + 1));
            t.setComments("Test comment for pagination " + (i + 1));
            t.setDate(randomDate());
            t.setOriginalAmount(10.0 + RANDOM.nextDouble() * 100.0);
            t.setOriginalCurrency("INR");
            t.setExchangeRate(1.0);
            t.setAccountId(account.getId());
            t.setCategoryId(category.getId());
            t.setTransactionType(TransactionDbType.DEBIT);
            Transaction saved = transactionRepository.save(t);
            ids.add(saved.getId());
        }
        // Flush to ensure all transactions are visible to subsequent queries
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
