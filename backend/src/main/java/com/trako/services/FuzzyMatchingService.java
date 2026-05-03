package com.trako.services;

import com.trako.models.MatchPosition;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FuzzyMatchingService {

    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    /**
     * Calculate similarity between two strings using Levenshtein distance.
     * Returns a score between 0.0 (completely different) and 1.0 (exact match).
     * Formula: 1.0 - (distance / max(len1, len2))
     */
    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.isEmpty() && s2.isEmpty()) {
            return 1.0;
        }
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        String lower1 = s1.toLowerCase();
        String lower2 = s2.toLowerCase();

        int distance = levenshteinDistance.apply(lower1, lower2);
        int maxLen = Math.max(lower1.length(), lower2.length());

        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Check if a text contains a fuzzy match for the query.
     * Slides a window of query length across the text and checks
     * if any window has similarity >= threshold.
     *
     * @param text      the text to search within
     * @param query     the search query
     * @param threshold minimum similarity score (0.0-1.0) to consider a match
     * @return true if any window in the text matches the query at or above the threshold
     */
    public boolean fuzzyContains(String text, String query, double threshold) {
        if (text == null || query == null || text.isEmpty() || query.isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();

        // Exact substring check first (fast path)
        if (lowerText.contains(lowerQuery)) {
            return true;
        }

        int queryLen = lowerQuery.length();
        if (queryLen > lowerText.length()) {
            // Compare the full strings when query is longer than text
            return calculateSimilarity(text, query) >= threshold;
        }

        // Slide a window of query length across the text
        for (int i = 0; i <= lowerText.length() - queryLen; i++) {
            String window = lowerText.substring(i, i + queryLen);
            double similarity = calculateSimilarity(window, lowerQuery);
            if (similarity >= threshold) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find all fuzzy matches in text and return their positions.
     * Similar to fuzzyContains but returns all match positions as List of MatchPosition.
     *
     * @param text      the text to search within
     * @param query     the search query
     * @param threshold minimum similarity score (0.0-1.0) to consider a match
     * @return list of match positions with similarity scores
     */
    public List<MatchPosition> findFuzzyMatches(String text, String query, double threshold) {
        List<MatchPosition> matches = new ArrayList<>();

        if (text == null || query == null || text.isEmpty() || query.isEmpty()) {
            return matches;
        }

        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int queryLen = lowerQuery.length();

        if (queryLen > lowerText.length()) {
            // Compare the full strings when query is longer than text
            double similarity = calculateSimilarity(text, query);
            if (similarity >= threshold) {
                matches.add(new MatchPosition(0, text.length(), similarity, text));
            }
            return matches;
        }

        // Slide a window of query length across the text
        for (int i = 0; i <= lowerText.length() - queryLen; i++) {
            String window = lowerText.substring(i, i + queryLen);
            double similarity = calculateSimilarity(window, lowerQuery);
            if (similarity >= threshold) {
                String matchedText = text.substring(i, i + queryLen);
                matches.add(new MatchPosition(i, i + queryLen, similarity, matchedText));

                // Skip ahead past this match to avoid overlapping matches
                i += queryLen - 1;
            }
        }

        return matches;
    }
}
