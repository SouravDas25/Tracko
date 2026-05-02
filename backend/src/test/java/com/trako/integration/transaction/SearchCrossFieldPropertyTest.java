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
 * Property-based test for Cross-Field Search Completeness.
 *
 * <p><b>Property 2: Cross-Field Search Completeness</b></p>
 * For any transaction where the search term appears in any searchable field,
 * verify the transaction is included in search results when the match meets
 * the fuzzy threshold.
 *
 * <p><b>Validates: Requirements 1.2, 2.1, 2.2</b></p>
 *
 * <p>Uses repeated tests with randomized unique search terms to simulate
 * property-based testing behavior. Transactions are created with a unique
 * search term placed in different fields (name, comments), then verified
 * that the full search flow via TransactionSearchService finds them all.</p>
 */
@Transactional
public class SearchCrossFieldPropertyTest extends BaseIntegrationTest {

    @Autowired
    private TransactionSearchService transactionSearchService;

    private static final Random RANDOM = new Random();

    private User user;
    private Account account;
    private Category category;

    @BeforeEach
    public void setup() {
        user = createUniqueUser("CrossField Property User");

        account = new Account();
        account.setName("CrossField Account");
        account.setUserId(user.getId());
        account.setCurrency("INR");
        account = accountRepository.save(account);

        category = new Category();
        category.setName("CrossField Category");
        category.setUserId(user.getId());
        category.setCategoryType(CategoryType.EXPENSE);
        category = categoryRepository.save(category);
    }

    /**
     * Property: a transaction with the search term in its NAME is found.
     * Creates a transaction where only the name field contains the unique
     * search term, then verifies the search service returns it.
     */
    @RepeatedTest(5)
    public void transactionWithSearchTermInNameIsFound() {
        String uniqueTerm = generateUniqueTerm();

        // Create a transaction with the unique term only in the name
        Transaction nameTransaction = createTransaction(
                uniqueTerm + " expense item",
                "unrelated comment text",
                50.0);

        SearchRequestDTO request = buildSearchRequest(uniqueTerm);
        TransactionSearchResultDTO result = transactionSearchService.search(user.getId(), request);

        Set<Long> returnedIds = extractTransactionIds(result);

        assertTrue(returnedIds.contains(nameTransaction.getId()),
                "Transaction with search term '" + uniqueTerm + "' in NAME (id="
                        + nameTransaction.getId() + ") should be found by search service");
        assertTrue(result.getTotalResults() >= 1,
                "At least 1 result expected when searching for term in name field");
    }

    /**
     * Property: a transaction with the search term in its COMMENTS is found.
     * Creates a transaction where only the comments field contains the unique
     * search term, then verifies the search service returns it.
     */
    @RepeatedTest(5)
    public void transactionWithSearchTermInCommentsIsFound() {
        String uniqueTerm = generateUniqueTerm();

        // Create a transaction with the unique term only in comments
        Transaction commentsTransaction = createTransaction(
                "generic payment",
                "note about " + uniqueTerm + " purchase",
                75.0);

        SearchRequestDTO request = buildSearchRequest(uniqueTerm);
        TransactionSearchResultDTO result = transactionSearchService.search(user.getId(), request);

        Set<Long> returnedIds = extractTransactionIds(result);

        assertTrue(returnedIds.contains(commentsTransaction.getId()),
                "Transaction with search term '" + uniqueTerm + "' in COMMENTS (id="
                        + commentsTransaction.getId() + ") should be found by search service");
        assertTrue(result.getTotalResults() >= 1,
                "At least 1 result expected when searching for term in comments field");
    }

    /**
     * Property: both name and comments matches are returned in a single search.
     * Creates two transactions — one with the search term in name, another with
     * it in comments — then verifies both appear in the results.
     */
    @RepeatedTest(5)
    public void bothNameAndCommentsMatchesAreReturned() {
        String uniqueTerm = generateUniqueTerm();

        // Transaction with term in name only
        Transaction nameTransaction = createTransaction(
                uniqueTerm + " store visit",
                "paid with card",
                30.0);

        // Transaction with term in comments only
        Transaction commentsTransaction = createTransaction(
                "regular purchase",
                "related to " + uniqueTerm + " order",
                60.0);

        SearchRequestDTO request = buildSearchRequest(uniqueTerm);
        TransactionSearchResultDTO result = transactionSearchService.search(user.getId(), request);

        Set<Long> returnedIds = extractTransactionIds(result);

        assertTrue(returnedIds.contains(nameTransaction.getId()),
                "Transaction with search term '" + uniqueTerm + "' in NAME (id="
                        + nameTransaction.getId() + ") should be in results");
        assertTrue(returnedIds.contains(commentsTransaction.getId()),
                "Transaction with search term '" + uniqueTerm + "' in COMMENTS (id="
                        + commentsTransaction.getId() + ") should be in results");
        assertTrue(result.getTotalResults() >= 2,
                "At least 2 results expected when term appears in name and comments of different transactions");
    }

    /**
     * Property: a transaction with the search term in both name and comments
     * is found and reports matches in both fields.
     */
    @RepeatedTest(3)
    public void transactionWithTermInBothFieldsIsFoundWithBothMatched() {
        String uniqueTerm = generateUniqueTerm();

        // Transaction with term in both name and comments
        Transaction bothFieldsTransaction = createTransaction(
                uniqueTerm + " dinner",
                "enjoyed " + uniqueTerm + " restaurant",
                45.0);

        SearchRequestDTO request = buildSearchRequest(uniqueTerm);
        TransactionSearchResultDTO result = transactionSearchService.search(user.getId(), request);

        Set<Long> returnedIds = extractTransactionIds(result);

        assertTrue(returnedIds.contains(bothFieldsTransaction.getId()),
                "Transaction with search term '" + uniqueTerm + "' in BOTH name and comments (id="
                        + bothFieldsTransaction.getId() + ") should be found");

        // Verify the matched fields include both name and comments
        Optional<TransactionSearchHitDTO> hit = result.getResults().stream()
                .filter(h -> h.getTransaction().getId().equals(bothFieldsTransaction.getId()))
                .findFirst();

        assertTrue(hit.isPresent(), "Hit for transaction should be present in results");
        List<String> matchedFields = hit.get().getMatchedFields();
        assertNotNull(matchedFields, "matchedFields should not be null");
        assertTrue(matchedFields.contains("name"),
                "matchedFields should include 'name' when term appears in name. Actual: " + matchedFields);
        assertTrue(matchedFields.contains("comments"),
                "matchedFields should include 'comments' when term appears in comments. Actual: " + matchedFields);
    }

    /**
     * Property: transactions without the search term in any field are NOT returned.
     * Creates a transaction with unrelated content and verifies it does not appear
     * in results for the unique search term.
     */
    @RepeatedTest(3)
    public void transactionWithoutSearchTermIsNotReturned() {
        String uniqueTerm = generateUniqueTerm();

        // Transaction that does NOT contain the unique term
        Transaction unrelatedTransaction = createTransaction(
                "completely different name",
                "no matching content here",
                100.0);

        // Transaction that DOES contain the unique term (to ensure search returns something)
        Transaction matchingTransaction = createTransaction(
                uniqueTerm + " item",
                "some comment",
                25.0);

        SearchRequestDTO request = buildSearchRequest(uniqueTerm);
        TransactionSearchResultDTO result = transactionSearchService.search(user.getId(), request);

        Set<Long> returnedIds = extractTransactionIds(result);

        assertTrue(returnedIds.contains(matchingTransaction.getId()),
                "Matching transaction should be returned");
        assertFalse(returnedIds.contains(unrelatedTransaction.getId()),
                "Transaction without search term '" + uniqueTerm + "' in any field (id="
                        + unrelatedTransaction.getId() + ") should NOT be in results");
    }

    // ---- Helper methods ----

    /**
     * Generate a unique search term that won't collide with other test data.
     * Uses a random alphabetic string to ensure uniqueness across repeated tests.
     */
    private String generateUniqueTerm() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder("xfld");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Create and persist a transaction with the given name, comments, and amount.
     */
    private Transaction createTransaction(String name, String comments, double amount) {
        Transaction t = new Transaction();
        t.setName(name);
        t.setComments(comments);
        t.setDate(new Date());
        t.setOriginalAmount(amount);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setAccountId(account.getId());
        t.setCategoryId(category.getId());
        t.setTransactionType(TransactionDbType.DEBIT);
        return transactionRepository.save(t);
    }

    /**
     * Build a SearchRequestDTO for the given query with a large page size
     * and the default fuzzy threshold.
     */
    private SearchRequestDTO buildSearchRequest(String query) {
        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery(query);
        request.setPage(0);
        request.setSize(100);
        request.setFuzzyThreshold(0.7);
        return request;
    }

    /**
     * Extract transaction IDs from search results.
     */
    private Set<Long> extractTransactionIds(TransactionSearchResultDTO result) {
        if (result.getResults() == null) {
            return Collections.emptySet();
        }
        return result.getResults().stream()
                .map(hit -> hit.getTransaction().getId())
                .collect(Collectors.toSet());
    }
}
