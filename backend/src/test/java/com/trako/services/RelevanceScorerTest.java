package com.trako.services;

import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.enums.CategoryType;
import com.trako.enums.TransactionDbType;
import com.trako.models.MatchPosition;
import com.trako.models.ScoredTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RelevanceScorer.
 * Validates: Requirements 1.4 (relevance ordering), 2.3 (field weighting)
 */
@ExtendWith(MockitoExtension.class)
public class RelevanceScorerTest {

    @Mock
    private FuzzyMatchingService fuzzyMatchingService;

    @InjectMocks
    private RelevanceScorer relevanceScorer;

    // --- Helper methods ---

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

    // --- Test: Name match scores higher than amount match ---
    // NAME_WEIGHT=1.0 vs AMOUNT_WEIGHT=0.5, so a name match should produce a higher score

    @Test
    public void calculateRelevance_nameMatchScoresHigherThanAmountMatch() {
        Transaction nameMatchTx = createTransaction(1L, "coffee", null, 50.0);
        Transaction amountMatchTx = createTransaction(2L, "groceries", null, 100.0);

        List<String> queryTokens = List.of("coffee");
        double threshold = 0.6;

        // Default: no matches for any string
        when(fuzzyMatchingService.calculateSimilarity(anyString(), eq("coffee"))).thenReturn(0.0);
        when(fuzzyMatchingService.findFuzzyMatches(anyString(), eq("coffee"), eq(threshold))).thenReturn(Collections.emptyList());

        // For nameMatchTx: name "coffee" matches token "coffee" with high similarity
        when(fuzzyMatchingService.calculateSimilarity("coffee", "coffee")).thenReturn(1.0);

        double nameScore = relevanceScorer.calculateRelevance(nameMatchTx, null, queryTokens, threshold);
        double amountScore = relevanceScorer.calculateRelevance(amountMatchTx, null, queryTokens, threshold);

        assertThat(nameScore).isGreaterThan(amountScore);
    }

    // --- Test: Multiple field matches increase score ---

    @Test
    public void calculateRelevance_multipleFieldMatchesIncreaseScore() {
        Transaction singleFieldTx = createTransaction(1L, "coffee", "no match here", 0.0);
        Transaction multiFieldTx = createTransaction(2L, "coffee", "coffee shop visit", 0.0);

        List<String> queryTokens = List.of("coffee");
        double threshold = 0.6;

        // Default: no matches for anything
        when(fuzzyMatchingService.calculateSimilarity(anyString(), eq("coffee"))).thenReturn(0.0);
        when(fuzzyMatchingService.findFuzzyMatches(anyString(), eq("coffee"), eq(threshold))).thenReturn(Collections.emptyList());

        // singleFieldTx: only name matches
        when(fuzzyMatchingService.calculateSimilarity("coffee", "coffee")).thenReturn(1.0);

        // singleFieldTx comments "no match here" — no match (already covered by default)
        // multiFieldTx comments "coffee shop visit" — contains "coffee" via fuzzy
        when(fuzzyMatchingService.calculateSimilarity("coffee shop visit", "coffee")).thenReturn(0.3);
        when(fuzzyMatchingService.findFuzzyMatches("coffee shop visit", "coffee", threshold))
                .thenReturn(List.of(new MatchPosition(0, 6, 1.0, "coffee")));

        double singleFieldScore = relevanceScorer.calculateRelevance(singleFieldTx, null, queryTokens, threshold);
        double multiFieldScore = relevanceScorer.calculateRelevance(multiFieldTx, null, queryTokens, threshold);

        assertThat(multiFieldScore).isGreaterThan(singleFieldScore);
    }

    // --- Test: rankByRelevance returns results sorted by relevance descending ---

    @Test
    public void rankByRelevance_returnsSortedByRelevanceDescending() {
        Transaction lowTx = createTransaction(1L, "groceries", null, 20.0);
        Transaction highTx = createTransaction(2L, "coffee", null, 50.0);
        Transaction midTx = createTransaction(3L, "cofee", null, 30.0); // typo — partial match

        List<String> queryTokens = List.of("coffee");
        double threshold = 0.6;

        // Default: no matches
        when(fuzzyMatchingService.calculateSimilarity(anyString(), eq("coffee"))).thenReturn(0.0);
        when(fuzzyMatchingService.findFuzzyMatches(anyString(), eq("coffee"), eq(threshold))).thenReturn(Collections.emptyList());
        when(fuzzyMatchingService.fuzzyContains(anyString(), eq("coffee"), eq(threshold))).thenReturn(false);

        // highTx: exact name match "coffee" -> similarity 1.0
        when(fuzzyMatchingService.calculateSimilarity("coffee", "coffee")).thenReturn(1.0);
        when(fuzzyMatchingService.fuzzyContains("coffee", "coffee", threshold)).thenReturn(true);
        when(fuzzyMatchingService.findFuzzyMatches("coffee", "coffee", threshold))
                .thenReturn(List.of(new MatchPosition(0, 6, 1.0, "coffee")));

        // midTx: partial name match "cofee" -> similarity 0.83
        when(fuzzyMatchingService.calculateSimilarity("cofee", "coffee")).thenReturn(0.83);
        when(fuzzyMatchingService.fuzzyContains("cofee", "coffee", threshold)).thenReturn(true);
        when(fuzzyMatchingService.findFuzzyMatches("cofee", "coffee", threshold))
                .thenReturn(List.of(new MatchPosition(0, 5, 0.83, "cofee")));

        // lowTx: name "groceries" — no match (covered by default)

        List<Transaction> transactions = List.of(lowTx, highTx, midTx);
        Map<Long, Category> categoryMap = Collections.emptyMap();

        List<ScoredTransaction> results = relevanceScorer.rankByRelevance(transactions, categoryMap, queryTokens, threshold);

        assertThat(results).hasSize(3);
        // Verify descending order
        assertThat(results.get(0).getTransaction().getId()).isEqualTo(2L); // highTx — exact match
        assertThat(results.get(1).getTransaction().getId()).isEqualTo(3L); // midTx — partial match
        assertThat(results.get(0).getRelevanceScore()).isGreaterThanOrEqualTo(results.get(1).getRelevanceScore());
        assertThat(results.get(1).getRelevanceScore()).isGreaterThanOrEqualTo(results.get(2).getRelevanceScore());
    }

    // --- Test: Null transaction returns 0.0 score ---

    @Test
    public void calculateRelevance_nullTransaction_returnsZero() {
        double score = relevanceScorer.calculateRelevance(null, null, List.of("coffee"), 0.6);
        assertThat(score).isEqualTo(0.0);
    }

    // --- Test: Empty query tokens returns 0.0 score ---

    @Test
    public void calculateRelevance_emptyQueryTokens_returnsZero() {
        Transaction tx = createTransaction(1L, "coffee", null, 50.0);
        double score = relevanceScorer.calculateRelevance(tx, null, Collections.emptyList(), 0.6);
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    public void calculateRelevance_nullQueryTokens_returnsZero() {
        Transaction tx = createTransaction(1L, "coffee", null, 50.0);
        double score = relevanceScorer.calculateRelevance(tx, null, null, 0.6);
        assertThat(score).isEqualTo(0.0);
    }
}
