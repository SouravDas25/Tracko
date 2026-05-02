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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for Search Authorization Boundary.
 *
 * <p><b>Property 1: Search Authorization Boundary</b></p>
 * For any search request and user, verify results only contain transactions
 * from accounts owned by that user.
 *
 * <p><b>Validates: Requirements 1.1, 1.2</b></p>
 *
 * <p>Uses repeated parameterized tests with randomized data to simulate
 * property-based testing behavior across multiple iterations.</p>
 */
@Transactional
public class SearchAuthorizationPropertyTest extends BaseIntegrationTest {

    @Autowired
    private TransactionSearchRepository transactionSearchRepository;

    private static final Random RANDOM = new Random();

    /** Shared search terms used across both users' transactions. */
    private static final String[] SHARED_TERMS = {
            "grocery", "coffee", "rent", "dinner", "shopping",
            "travel", "medical", "insurance", "utility", "subscription"
    };

    private User userA;
    private User userB;
    private List<Account> userAAccounts;
    private List<Account> userBAccounts;
    private Set<Long> userAAccountIds;
    private Set<Long> userBAccountIds;
    private Category categoryA;
    private Category categoryB;

    @BeforeEach
    public void setup() {
        userA = createUniqueUser("Auth Property User A");
        userB = createUniqueUser("Auth Property User B");

        // Create multiple accounts per user to test across account boundaries
        userAAccounts = createAccountsForUser(userA, 2);
        userBAccounts = createAccountsForUser(userB, 2);

        userAAccountIds = userAAccounts.stream().map(Account::getId).collect(Collectors.toSet());
        userBAccountIds = userBAccounts.stream().map(Account::getId).collect(Collectors.toSet());

        categoryA = createCategory(userA, "General A");
        categoryB = createCategory(userB, "General B");

        // Populate both users with transactions that share overlapping search terms
        populateTransactions(userAAccounts, categoryA, 10);
        populateTransactions(userBAccounts, categoryB, 10);
    }

    /**
     * Property: searching with a random shared term returns only the searching user's transactions.
     * Repeated 5 times with different random search terms to cover multiple input scenarios.
     */
    @RepeatedTest(5)
    public void searchResultsContainOnlySearchingUsersTransactions() {
        String searchTerm = SHARED_TERMS[RANDOM.nextInt(SHARED_TERMS.length)];

        Page<Transaction> resultsA = transactionSearchRepository.searchTransactions(
                userA.getId(), List.of(searchTerm), null, null, null, null, null, null,
                PageRequest.of(0, 100));

        // Every returned transaction must belong to an account owned by user A
        for (Transaction tx : resultsA.getContent()) {
            assertTrue(userAAccountIds.contains(tx.getAccountId()),
                    "User A search returned transaction (id=" + tx.getId()
                            + ") from account " + tx.getAccountId()
                            + " which does not belong to User A. Search term: '" + searchTerm + "'");
            assertFalse(userBAccountIds.contains(tx.getAccountId()),
                    "User A search returned transaction belonging to User B's account");
        }

        Page<Transaction> resultsB = transactionSearchRepository.searchTransactions(
                userB.getId(), List.of(searchTerm), null, null, null, null, null, null,
                PageRequest.of(0, 100));

        // Every returned transaction must belong to an account owned by user B
        for (Transaction tx : resultsB.getContent()) {
            assertTrue(userBAccountIds.contains(tx.getAccountId()),
                    "User B search returned transaction (id=" + tx.getId()
                            + ") from account " + tx.getAccountId()
                            + " which does not belong to User B. Search term: '" + searchTerm + "'");
            assertFalse(userAAccountIds.contains(tx.getAccountId()),
                    "User B search returned transaction belonging to User A's account");
        }
    }

    /**
     * Property: empty search tokens still respect authorization boundaries.
     * When no query tokens are provided, all returned transactions must still
     * belong to the requesting user.
     */
    @RepeatedTest(3)
    public void emptyQueryRespectsAuthorizationBoundary() {
        Page<Transaction> resultsA = transactionSearchRepository.searchTransactions(
                userA.getId(), List.of(), null, null, null, null, null, null,
                PageRequest.of(0, 100));

        for (Transaction tx : resultsA.getContent()) {
            assertTrue(userAAccountIds.contains(tx.getAccountId()),
                    "Empty-query search for User A returned transaction from account "
                            + tx.getAccountId() + " not owned by User A");
        }

        Page<Transaction> resultsB = transactionSearchRepository.searchTransactions(
                userB.getId(), List.of(), null, null, null, null, null, null,
                PageRequest.of(0, 100));

        for (Transaction tx : resultsB.getContent()) {
            assertTrue(userBAccountIds.contains(tx.getAccountId()),
                    "Empty-query search for User B returned transaction from account "
                            + tx.getAccountId() + " not owned by User B");
        }
    }

    /**
     * Property: filtering by account IDs still cannot leak another user's data.
     * Even if a malicious caller passes the other user's account IDs, the
     * authorization subquery should prevent any cross-user leakage.
     */
    @RepeatedTest(3)
    public void filteringByOtherUsersAccountIdsReturnsNothing() {
        // User A tries to search with User B's account IDs
        List<Long> userBAccountIdList = userBAccounts.stream()
                .map(Account::getId).collect(Collectors.toList());

        Page<Transaction> results = transactionSearchRepository.searchTransactions(
                userA.getId(), List.of(), null, null, null, null,
                userBAccountIdList, null,
                PageRequest.of(0, 100));

        assertEquals(0, results.getTotalElements(),
                "User A should get zero results when filtering by User B's account IDs");
    }

    /**
     * Property: combined filters (date range, amount range) still enforce authorization.
     * Randomized date and amount ranges should never return another user's transactions.
     */
    @RepeatedTest(3)
    public void combinedFiltersRespectAuthorizationBoundary() {
        String searchTerm = SHARED_TERMS[RANDOM.nextInt(SHARED_TERMS.length)];

        // Random date range within 2024-2025
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 1, 0, 0, 0);
        Date startDate = cal.getTime();
        cal.set(2025, Calendar.DECEMBER, 31, 23, 59, 59);
        Date endDate = cal.getTime();

        // Random amount range
        double minAmount = RANDOM.nextDouble() * 50;
        double maxAmount = minAmount + 50 + RANDOM.nextDouble() * 200;

        Page<Transaction> resultsA = transactionSearchRepository.searchTransactions(
                userA.getId(), List.of(searchTerm), startDate, endDate,
                minAmount, maxAmount, null, null,
                PageRequest.of(0, 100));

        for (Transaction tx : resultsA.getContent()) {
            assertTrue(userAAccountIds.contains(tx.getAccountId()),
                    "Combined-filter search for User A returned transaction from account "
                            + tx.getAccountId() + " not owned by User A");
        }

        Page<Transaction> resultsB = transactionSearchRepository.searchTransactions(
                userB.getId(), List.of(searchTerm), startDate, endDate,
                minAmount, maxAmount, null, null,
                PageRequest.of(0, 100));

        for (Transaction tx : resultsB.getContent()) {
            assertTrue(userBAccountIds.contains(tx.getAccountId()),
                    "Combined-filter search for User B returned transaction from account "
                            + tx.getAccountId() + " not owned by User B");
        }
    }

    // ---- Helper methods ----

    private List<Account> createAccountsForUser(User user, int count) {
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Account account = new Account();
            account.setName(user.getName() + " Account " + (i + 1));
            account.setUserId(user.getId());
            account.setCurrency("INR");
            accounts.add(accountRepository.save(account));
        }
        return accounts;
    }

    private Category createCategory(User user, String name) {
        Category category = new Category();
        category.setName(name);
        category.setUserId(user.getId());
        category.setCategoryType(CategoryType.EXPENSE);
        return categoryRepository.save(category);
    }

    private void populateTransactions(List<Account> accounts, Category category, int count) {
        for (int i = 0; i < count; i++) {
            Account account = accounts.get(RANDOM.nextInt(accounts.size()));
            String term = SHARED_TERMS[RANDOM.nextInt(SHARED_TERMS.length)];
            String name = term + " payment " + UUID.randomUUID().toString().substring(0, 6);
            String comments = "Comment about " + SHARED_TERMS[RANDOM.nextInt(SHARED_TERMS.length)];

            Transaction t = new Transaction();
            t.setName(name);
            t.setComments(comments);
            t.setDate(randomDate());
            t.setOriginalAmount(10.0 + RANDOM.nextDouble() * 200.0);
            t.setOriginalCurrency("INR");
            t.setExchangeRate(1.0);
            t.setAccountId(account.getId());
            t.setCategoryId(category.getId());
            t.setTransactionType(TransactionDbType.DEBIT);
            transactionRepository.save(t);
        }
    }

    private Date randomDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024 + RANDOM.nextInt(2), RANDOM.nextInt(12),
                1 + RANDOM.nextInt(28), 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
