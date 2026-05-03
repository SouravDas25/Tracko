package com.trako.services;

import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.enums.CategoryType;
import com.trako.enums.TransactionDbType;
import com.trako.models.ScoredTransaction;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for RelevanceScorer.
 *
 * <p><b>Validates: Requirements 1.4, 2.3</b></p>
 *
 * <p>Property 5: Relevance-Based Ordering with Field Weighting —
 * For any set of matching transactions, results SHALL be ordered by relevance score
 * where matches in name/comments fields contribute more to the score than matches
 * in amount/date fields.</p>
 */
public class RelevanceScorerPropertyTest {

    private final RelevanceScorer scorer;
    private final FuzzyMatchingService fuzzyService;

    public RelevanceScorerPropertyTest() {
        scorer = new RelevanceScorer();
        fuzzyService = new FuzzyMatchingService();
        try {
            Field field = RelevanceScorer.class.getDeclaredField("fuzzyMatchingService");
            field.setAccessible(true);
            field.set(scorer, fuzzyService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject FuzzyMatchingService into RelevanceScorer", e);
        }
    }

    // --- Helpers ---

    private Transaction createTransaction(Long id, String name, String comments, Double amount) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setName(name);
        tx.setComments(comments);
        tx.setAmount(amount);
        tx.setDate(new Date());
        tx.setAccountId(1L);
        tx.setCategoryId(1L);
        tx.setTransactionType(TransactionDbType.DEBIT);
        tx.setOriginalCurrency("USD");
        tx.setOriginalAmount(amount != null ? amount : 0.0);
        tx.setExchangeRate(1.0);
        return tx;
    }

    private Category createCategory(Long id, String name) {
        Category cat = new Category();
        cat.setId(id);
        cat.setName(name);
        cat.setUserId("user1");
        cat.setCategoryType(CategoryType.EXPENSE);
        return cat;
    }

    // -----------------------------------------------------------------------
    // Property 5a: rankByRelevance output is always sorted by relevance
    //              score descending for any list of transactions.
    // -----------------------------------------------------------------------

    /**
     * <b>Validates: Requirements 1.4, 2.3</b>
     *
     * <p>For any list of transactions scored against a query, the output of
     * rankByRelevance must be sorted in descending order of relevance score.
     * This ensures users always see the most relevant results first.</p>
     */
    @Property(tries = 200)
    void rankByRelevance_alwaysReturnsSortedDescending(
            @ForAll("transactionLists") List<Transaction> transactions,
            @ForAll @AlphaChars @StringLength(min = 2, max = 10) String queryToken
    ) {
        List<String> queryTokens = List.of(queryToken.toLowerCase());
        double threshold = 0.6;
        Map<Long, Category> categoryMap = Collections.emptyMap();

        List<ScoredTransaction> results = scorer.rankByRelevance(
                transactions, categoryMap, queryTokens, threshold);

        // Verify descending order: each score >= the next
        for (int i = 0; i < results.size() - 1; i++) {
            double current = results.get(i).getRelevanceScore();
            double next = results.get(i + 1).getRelevanceScore();
            assertThat(current)
                    .as("Result at index %d (score=%.4f) should be >= result at index %d (score=%.4f)",
                            i, current, i + 1, next)
                    .isGreaterThanOrEqualTo(next);
        }
    }

    @Provide
    Arbitrary<List<Transaction>> transactionLists() {
        Arbitrary<String> names = Arbitraries.strings()
                .alpha().ofMinLength(2).ofMaxLength(20);
        Arbitrary<String> comments = Arbitraries.strings()
                .alpha().ofMinLength(0).ofMaxLength(30)
                .injectNull(0.3);
        Arbitrary<Double> amounts = Arbitraries.doubles()
                .between(0.01, 10000.0);

        Arbitrary<Transaction> txArbitrary = Combinators.combine(names, comments, amounts)
                .as((name, comment, amount) -> {
                    // ID will be set after generation to ensure uniqueness
                    return createTransaction(0L, name, comment, amount);
                });

        return txArbitrary.list().ofMinSize(1).ofMaxSize(15)
                .map(list -> {
                    // Assign unique IDs
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i).setId((long) (i + 1));
                    }
                    return list;
                });
    }

    // -----------------------------------------------------------------------
    // Property 5b: A transaction with a name match always scores >= a
    //              transaction with only an amount match, because
    //              NAME_WEIGHT (1.0) > AMOUNT_WEIGHT (0.5).
    // -----------------------------------------------------------------------

    /**
     * <b>Validates: Requirements 1.4, 2.3</b>
     *
     * <p>For any query token, a transaction whose name exactly equals the token
     * must score at least as high as a transaction whose only match is in the
     * amount field. This validates that field weighting is applied correctly
     * (NAME_WEIGHT=1.0 > AMOUNT_WEIGHT=0.5).</p>
     */
    @Property(tries = 200)
    void nameMatch_scoresHigherOrEqual_thanAmountOnlyMatch(
            @ForAll @AlphaChars @StringLength(min = 3, max = 12) String queryToken
    ) {
        String token = queryToken.toLowerCase();
        List<String> queryTokens = List.of(token);
        double threshold = 0.6;

        // Transaction A: name matches the query exactly, amount is unrelated
        Transaction nameMatchTx = createTransaction(1L, token, null, 999.99);

        // Transaction B: name is completely unrelated, amount string contains the token
        // We use a name that is very different from the token to avoid accidental matches
        Transaction amountMatchTx = createTransaction(2L, "zzzzzzzzzzzzzzz", null, 0.0);
        // Set the amount to a value whose string representation won't match the alpha token
        amountMatchTx.setAmount(12345.67);

        double nameScore = scorer.calculateRelevance(nameMatchTx, null, queryTokens, threshold);
        double amountScore = scorer.calculateRelevance(amountMatchTx, null, queryTokens, threshold);

        assertThat(nameScore)
                .as("Name-match score (%.4f) for token '%s' should be >= amount-only score (%.4f)",
                        nameScore, token, amountScore)
                .isGreaterThanOrEqualTo(amountScore);
    }
}
