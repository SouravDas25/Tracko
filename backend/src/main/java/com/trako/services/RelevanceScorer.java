package com.trako.services;

import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.models.MatchPosition;
import com.trako.models.ScoredTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelevanceScorer {

    // Field weights for relevance calculation
    private static final double NAME_WEIGHT = 1.0;
    private static final double COMMENTS_WEIGHT = 0.9;
    private static final double CATEGORY_WEIGHT = 0.7;
    private static final double AMOUNT_WEIGHT = 0.5;
    private static final double DATE_WEIGHT = 0.3;

    private static final String FIELD_NAME = "name";
    private static final String FIELD_COMMENTS = "comments";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_AMOUNT = "amount";
    private static final String FIELD_DATE = "date";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private FuzzyMatchingService fuzzyMatchingService;

    /**
     * Calculate relevance score for a transaction against search query tokens.
     * For each query token, checks each field and uses the best similarity
     * multiplied by the field weight. Sums across all fields and tokens.
     *
     * @param transaction   the transaction to score
     * @param category      the category of the transaction (may be null)
     * @param queryTokens   the tokenized search query
     * @param fuzzyThreshold minimum similarity score to consider a match
     * @return the total relevance score
     */
    public double calculateRelevance(Transaction transaction, Category category,
                                     List<String> queryTokens, double fuzzyThreshold) {
        if (transaction == null || queryTokens == null || queryTokens.isEmpty()) {
            return 0.0;
        }

        String nameValue = transaction.getName();
        String commentsValue = transaction.getComments();
        String categoryValue = (category != null) ? category.getName() : null;
        String amountValue = (transaction.getAmount() != null) ? transaction.getAmount().toString() : null;
        String dateValue = (transaction.getDate() != null) ? dateFormat.format(transaction.getDate()) : null;

        double totalScore = 0.0;

        for (String token : queryTokens) {
            totalScore += scoreField(nameValue, token, NAME_WEIGHT, fuzzyThreshold);
            totalScore += scoreField(commentsValue, token, COMMENTS_WEIGHT, fuzzyThreshold);
            totalScore += scoreField(categoryValue, token, CATEGORY_WEIGHT, fuzzyThreshold);
            totalScore += scoreField(amountValue, token, AMOUNT_WEIGHT, fuzzyThreshold);
            totalScore += scoreField(dateValue, token, DATE_WEIGHT, fuzzyThreshold);
        }

        return totalScore;
    }

    /**
     * Rank transactions by relevance score in descending order.
     * For each transaction, calculates the relevance score, tracks matched fields
     * and match positions, and returns a sorted list of ScoredTransaction.
     *
     * @param transactions  the list of transactions to rank
     * @param categoryMap   map of category ID to Category
     * @param queryTokens   the tokenized search query
     * @param fuzzyThreshold minimum similarity score to consider a match
     * @return sorted list of ScoredTransaction (highest relevance first)
     */
    public List<ScoredTransaction> rankByRelevance(List<Transaction> transactions,
                                                    Map<Long, Category> categoryMap,
                                                    List<String> queryTokens,
                                                    double fuzzyThreshold) {
        if (transactions == null || transactions.isEmpty()) {
            return Collections.emptyList();
        }
        if (queryTokens == null || queryTokens.isEmpty()) {
            return Collections.emptyList();
        }

        return transactions.stream()
                .map(transaction -> {
                    Category category = (categoryMap != null)
                            ? categoryMap.get(transaction.getCategoryId())
                            : null;

                    double relevanceScore = calculateRelevance(transaction, category, queryTokens, fuzzyThreshold);
                    List<String> matchedFields = findMatchedFields(transaction, category, queryTokens, fuzzyThreshold);
                    Map<String, List<MatchPosition>> matchPositions = findMatchPositions(transaction, category, queryTokens, fuzzyThreshold);

                    return new ScoredTransaction(transaction, category, relevanceScore, matchedFields, matchPositions);
                })
                .sorted(Comparator.comparingDouble(ScoredTransaction::getRelevanceScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Score a single field against a query token.
     * Uses fuzzy similarity and multiplies by the field weight.
     */
    private double scoreField(String fieldValue, String token, double weight, double fuzzyThreshold) {
        if (fieldValue == null || fieldValue.isEmpty() || token == null || token.isEmpty()) {
            return 0.0;
        }

        double similarity = fuzzyMatchingService.calculateSimilarity(fieldValue.toLowerCase(), token.toLowerCase());
        if (similarity >= fuzzyThreshold) {
            return similarity * weight;
        }

        // Also check if the field contains the token via fuzzy matching
        List<MatchPosition> matches = fuzzyMatchingService.findFuzzyMatches(fieldValue, token, fuzzyThreshold);
        if (!matches.isEmpty()) {
            double bestSimilarity = matches.stream()
                    .mapToDouble(MatchPosition::getSimilarity)
                    .max()
                    .orElse(0.0);
            return bestSimilarity * weight;
        }

        return 0.0;
    }

    /**
     * Find which fields matched for a transaction against the query tokens.
     */
    private List<String> findMatchedFields(Transaction transaction, Category category,
                                           List<String> queryTokens, double fuzzyThreshold) {
        List<String> matchedFields = new ArrayList<>();

        String nameValue = transaction.getName();
        String commentsValue = transaction.getComments();
        String categoryValue = (category != null) ? category.getName() : null;
        String amountValue = (transaction.getAmount() != null) ? transaction.getAmount().toString() : null;
        String dateValue = (transaction.getDate() != null) ? dateFormat.format(transaction.getDate()) : null;

        for (String token : queryTokens) {
            if (fieldMatches(nameValue, token, fuzzyThreshold) && !matchedFields.contains(FIELD_NAME)) {
                matchedFields.add(FIELD_NAME);
            }
            if (fieldMatches(commentsValue, token, fuzzyThreshold) && !matchedFields.contains(FIELD_COMMENTS)) {
                matchedFields.add(FIELD_COMMENTS);
            }
            if (fieldMatches(categoryValue, token, fuzzyThreshold) && !matchedFields.contains(FIELD_CATEGORY)) {
                matchedFields.add(FIELD_CATEGORY);
            }
            if (fieldMatches(amountValue, token, fuzzyThreshold) && !matchedFields.contains(FIELD_AMOUNT)) {
                matchedFields.add(FIELD_AMOUNT);
            }
            if (fieldMatches(dateValue, token, fuzzyThreshold) && !matchedFields.contains(FIELD_DATE)) {
                matchedFields.add(FIELD_DATE);
            }
        }

        return matchedFields;
    }

    /**
     * Check if a field value matches a token at or above the fuzzy threshold.
     */
    private boolean fieldMatches(String fieldValue, String token, double fuzzyThreshold) {
        if (fieldValue == null || fieldValue.isEmpty() || token == null || token.isEmpty()) {
            return false;
        }
        return fuzzyMatchingService.fuzzyContains(fieldValue, token, fuzzyThreshold);
    }

    /**
     * Find match positions for each field against the query tokens.
     */
    private Map<String, List<MatchPosition>> findMatchPositions(Transaction transaction, Category category,
                                                                 List<String> queryTokens, double fuzzyThreshold) {
        Map<String, List<MatchPosition>> matchPositions = new LinkedHashMap<>();

        String nameValue = transaction.getName();
        String commentsValue = transaction.getComments();
        String categoryValue = (category != null) ? category.getName() : null;
        String amountValue = (transaction.getAmount() != null) ? transaction.getAmount().toString() : null;
        String dateValue = (transaction.getDate() != null) ? dateFormat.format(transaction.getDate()) : null;

        collectFieldPositions(matchPositions, FIELD_NAME, nameValue, queryTokens, fuzzyThreshold);
        collectFieldPositions(matchPositions, FIELD_COMMENTS, commentsValue, queryTokens, fuzzyThreshold);
        collectFieldPositions(matchPositions, FIELD_CATEGORY, categoryValue, queryTokens, fuzzyThreshold);
        collectFieldPositions(matchPositions, FIELD_AMOUNT, amountValue, queryTokens, fuzzyThreshold);
        collectFieldPositions(matchPositions, FIELD_DATE, dateValue, queryTokens, fuzzyThreshold);

        return matchPositions;
    }

    /**
     * Collect match positions for a single field across all query tokens.
     */
    private void collectFieldPositions(Map<String, List<MatchPosition>> matchPositions,
                                       String fieldName, String fieldValue,
                                       List<String> queryTokens, double fuzzyThreshold) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            return;
        }

        List<MatchPosition> positions = new ArrayList<>();
        for (String token : queryTokens) {
            List<MatchPosition> tokenMatches = fuzzyMatchingService.findFuzzyMatches(fieldValue, token, fuzzyThreshold);
            positions.addAll(tokenMatches);
        }

        if (!positions.isEmpty()) {
            matchPositions.put(fieldName, positions);
        }
    }
}
