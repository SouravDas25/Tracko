package com.trako.integration.transaction;

import com.trako.dtos.SearchRequestDTO;
import com.trako.dtos.TransactionSearchResultDTO;
import com.trako.entities.Account;
import com.trako.entities.User;
import com.trako.integration.BaseIntegrationTest;
import com.trako.services.TransactionSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for Empty Result Handling.
 *
 * <p><b>Property 10: Empty Result Handling</b></p>
 * For any search query matching no transactions, verify empty result set
 * with totalResults=0 and appropriate metadata.
 *
 * <p><b>Validates: Requirements 1.5</b></p>
 *
 * <p>Uses repeated tests with randomized queries against a user who has
 * an account but NO transactions. Every random query should yield an
 * empty result set with correct metadata.</p>
 */
@Transactional
public class SearchEmptyResultPropertyTest extends BaseIntegrationTest {

    @Autowired
    private TransactionSearchService transactionSearchService;

    private static final Random RANDOM = new Random();

    private User user;
    private Account account;

    @BeforeEach
    public void setup() {
        user = createUniqueUser("EmptyResult Property User");

        account = new Account();
        account.setName("EmptyResult Account");
        account.setUserId(user.getId());
        account.setCurrency("INR");
        account = accountRepository.save(account);

        // Intentionally NO transactions created — every search should return empty results
    }

    /**
     * Property: for any random query, totalResults is 0 when no transactions exist.
     */
    @RepeatedTest(5)
    public void randomQueryReturnsZeroTotalResults() {
        String randomQuery = generateRandomQuery();

        SearchRequestDTO request = buildSearchRequest(randomQuery);
        TransactionSearchResultDTO result = transactionSearchService.search(user.getId(), request);

        assertEquals(0L, result.getTotalResults(),
                "totalResults should be 0 for query '" + randomQuery + "' when user has no transactions");
    }

    /**
     * Property: for any random query, the results list is empty when no transactions exist.
     */
    @RepeatedTest(5)
    public void randomQueryReturnsEmptyResultsList() {
        String randomQuery = generateRandomQuery();

        SearchRequestDTO request = buildSearchRequest(randomQuery);
        TransactionSearchResultDTO result = transactionSearchService.search(user.getId(), request);

        assertNotNull(result.getResults(),
                "Results list should not be null for query '" + randomQuery + "'");
        assertTrue(result.getResults().isEmpty(),
                "Results list should be empty for query '" + randomQuery + "' when user has no transactions");
    }

    /**
     * Property: for any random query with no matches, metadata is correct
     * (page=0, hasNext=false, hasPrevious=false, searchTimeMs >= 0).
     */
    @RepeatedTest(5)
    public void randomQueryReturnsCorrectMetadata() {
        String randomQuery = generateRandomQuery();

        SearchRequestDTO request = buildSearchRequest(randomQuery);
        TransactionSearchResultDTO result = transactionSearchService.search(user.getId(), request);

        assertEquals(0, result.getPage(),
                "Page should be 0 for empty result with query '" + randomQuery + "'");
        assertFalse(result.getHasNext(),
                "hasNext should be false for empty result with query '" + randomQuery + "'");
        assertFalse(result.getHasPrevious(),
                "hasPrevious should be false for empty result with query '" + randomQuery + "'");
        assertNotNull(result.getSearchTimeMs(),
                "searchTimeMs should not be null for query '" + randomQuery + "'");
        assertTrue(result.getSearchTimeMs() >= 0,
                "searchTimeMs should be >= 0 for query '" + randomQuery + "', got: " + result.getSearchTimeMs());
    }

    /**
     * Property: for any random query, the query field in the result matches the input query.
     */
    @RepeatedTest(5)
    public void resultQueryFieldMatchesInputQuery() {
        String randomQuery = generateRandomQuery();

        SearchRequestDTO request = buildSearchRequest(randomQuery);
        TransactionSearchResultDTO result = transactionSearchService.search(user.getId(), request);

        assertEquals(randomQuery, result.getQuery(),
                "Result query field should match the input query '" + randomQuery + "'");
    }

    // ---- Helper methods ----

    /**
     * Generate a random query string that won't match any real data.
     * Uses a random alphabetic prefix to ensure uniqueness across repeated tests.
     */
    private String generateRandomQuery() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder("zempty");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Build a SearchRequestDTO for the given query with default pagination settings.
     */
    private SearchRequestDTO buildSearchRequest(String query) {
        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery(query);
        request.setPage(0);
        request.setSize(20);
        request.setFuzzyThreshold(0.7);
        return request;
    }
}
