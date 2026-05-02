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
 * Property-based test for Index Freshness (New Transaction Searchability).
 *
 * <p><b>Property 9: Index Freshness (New Transaction Searchability)</b></p>
 * For any newly created transaction, verify it's immediately searchable
 * in subsequent search requests.
 *
 * <p><b>Validates: Requirements 5.3</b></p>
 *
 * <p>Uses repeated tests with randomized data to simulate property-based
 * testing behavior across multiple iterations. Each iteration creates a
 * transaction with a unique name and immediately searches for it.</p>
 */
@Transactional
public class SearchIndexFreshnessPropertyTest extends BaseIntegrationTest {

    @Autowired
    private TransactionSearchService transactionSearchService;

    private static final Random RANDOM = new Random();

    private User user;
    private Account account;
    private Category category;

    @BeforeEach
    public void setup() {
        user = createUniqueUser("Index Freshness Property User");

        account = new Account();
        account.setName("Freshness Test Account");
        account.setUserId(user.getId());
        account.setCurrency("INR");
        account = accountRepository.save(account);

        category = new Category();
        category.setName("Freshness Category");
        category.setUserId(user.getId());
        category.setCategoryType(CategoryType.EXPENSE);
        category = categoryRepository.save(category);
    }

    /**
     * Property: a newly created transaction is immediately searchable by its
     * unique name in a subsequent search request.
     *
     * <p>Creates a single transaction with a globally unique name, then
     * immediately searches for that name and verifies the transaction
     * appears in the results.</p>
     */
    @RepeatedTest(5)
    public void newlyCreatedTransactionIsImmediatelySearchable() {
        // Generate a unique name that won't collide with other test data
        String uniqueName = "freshness_" + UUID.randomUUID().toString().substring(0, 12);

        // Create the transaction
        Transaction transaction = new Transaction();
        transaction.setName(uniqueName);
        transaction.setComments("Freshness test comment");
        transaction.setDate(randomDate());
        transaction.setOriginalAmount(10.0 + RANDOM.nextDouble() * 500.0);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setAccountId(account.getId());
        transaction.setCategoryId(category.getId());
        transaction.setTransactionType(TransactionDbType.DEBIT);
        Transaction saved = transactionRepository.save(transaction);
        transactionRepository.flush();

        assertNotNull(saved.getId(), "Saved transaction should have an ID");

        // Immediately search for the unique name
        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery(uniqueName);
        request.setPage(0);
        request.setSize(10);

        TransactionSearchResultDTO result = transactionSearchService.search(
                user.getId(), request);

        // Verify the transaction appears in results
        assertNotNull(result, "Search result should not be null");
        assertNotNull(result.getResults(), "Results list should not be null");
        assertFalse(result.getResults().isEmpty(),
                "Newly created transaction with name '" + uniqueName
                        + "' should be immediately searchable, but search returned no results");

        Set<Long> resultIds = result.getResults().stream()
                .map(hit -> hit.getTransaction().getId())
                .collect(Collectors.toSet());

        assertTrue(resultIds.contains(saved.getId()),
                "Search results should contain the newly created transaction (ID="
                        + saved.getId() + "). Found IDs: " + resultIds);
    }

    /**
     * Property: multiple transactions created in sequence are all immediately
     * searchable using a shared unique term.
     *
     * <p>Creates several transactions with a common unique prefix, then
     * searches for that prefix and verifies all of them appear.</p>
     */
    @RepeatedTest(3)
    public void multipleNewTransactionsAreAllImmediatelySearchable() {
        String sharedPrefix = "batchfresh_" + UUID.randomUUID().toString().substring(0, 8);
        int count = 3 + RANDOM.nextInt(3); // 3 to 5 transactions

        Set<Long> createdIds = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            Transaction transaction = new Transaction();
            transaction.setName(sharedPrefix + " item " + (i + 1));
            transaction.setComments("Batch freshness comment " + (i + 1));
            transaction.setDate(randomDate());
            transaction.setOriginalAmount(5.0 + RANDOM.nextDouble() * 200.0);
            transaction.setOriginalCurrency("INR");
            transaction.setExchangeRate(1.0);
            transaction.setAccountId(account.getId());
            transaction.setCategoryId(category.getId());
            transaction.setTransactionType(TransactionDbType.DEBIT);
            Transaction saved = transactionRepository.save(transaction);
            createdIds.add(saved.getId());
        }
        transactionRepository.flush();

        // Immediately search for the shared prefix
        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery(sharedPrefix);
        request.setPage(0);
        request.setSize(count + 10); // generous page size

        TransactionSearchResultDTO result = transactionSearchService.search(
                user.getId(), request);

        assertNotNull(result, "Search result should not be null");
        assertNotNull(result.getResults(), "Results list should not be null");

        Set<Long> resultIds = result.getResults().stream()
                .map(hit -> hit.getTransaction().getId())
                .collect(Collectors.toSet());

        assertEquals(createdIds.size(), resultIds.size(),
                "All " + createdIds.size() + " newly created transactions should be "
                        + "immediately searchable. Expected IDs: " + createdIds
                        + ", Found IDs: " + resultIds);

        assertTrue(resultIds.containsAll(createdIds),
                "Search results should contain all newly created transaction IDs. "
                        + "Missing: " + createdIds.stream()
                        .filter(id -> !resultIds.contains(id))
                        .collect(Collectors.toSet()));
    }

    /**
     * Property: a newly created transaction is searchable by its comments
     * field, not just its name.
     *
     * <p>Creates a transaction with a unique comment and searches for that
     * comment to verify cross-field index freshness.</p>
     */
    @RepeatedTest(3)
    public void newTransactionIsSearchableByComments() {
        String uniqueComment = "commentfresh_" + UUID.randomUUID().toString().substring(0, 12);

        Transaction transaction = new Transaction();
        transaction.setName("Generic Transaction Name");
        transaction.setComments(uniqueComment);
        transaction.setDate(randomDate());
        transaction.setOriginalAmount(25.0 + RANDOM.nextDouble() * 100.0);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setAccountId(account.getId());
        transaction.setCategoryId(category.getId());
        transaction.setTransactionType(TransactionDbType.DEBIT);
        Transaction saved = transactionRepository.save(transaction);
        transactionRepository.flush();

        // Search by the unique comment
        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery(uniqueComment);
        request.setPage(0);
        request.setSize(10);

        TransactionSearchResultDTO result = transactionSearchService.search(
                user.getId(), request);

        assertNotNull(result, "Search result should not be null");
        assertNotNull(result.getResults(), "Results list should not be null");
        assertFalse(result.getResults().isEmpty(),
                "Newly created transaction should be immediately searchable by its "
                        + "comments field ('" + uniqueComment + "')");

        Set<Long> resultIds = result.getResults().stream()
                .map(hit -> hit.getTransaction().getId())
                .collect(Collectors.toSet());

        assertTrue(resultIds.contains(saved.getId()),
                "Search results should contain the newly created transaction (ID="
                        + saved.getId() + ") when searching by comments");
    }

    // ---- Helper methods ----

    private Date randomDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, RANDOM.nextInt(12), 1 + RANDOM.nextInt(28), 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
