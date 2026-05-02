package com.trako.services;

import com.trako.models.MatchPosition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class HighlightGenerator {

    @Autowired
    private FuzzyMatchingService fuzzyMatchingService;

    /**
     * Generate highlighted text by wrapping matched portions with &lt;em&gt; markers.
     * Finds all match positions for the given query tokens, merges overlapping
     * positions, and inserts highlight markers around matched text.
     *
     * @param text        the original text to highlight
     * @param queryTokens the search query tokens to match against
     * @param threshold   minimum similarity score (0.0-1.0) to consider a match
     * @return the text with &lt;em&gt; and &lt;/em&gt; markers around matched portions,
     *         or the original text if no matches are found
     */
    public String generateHighlights(String text, List<String> queryTokens, double threshold) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (queryTokens == null || queryTokens.isEmpty()) {
            return text;
        }

        List<MatchPosition> positions = findMatchPositions(text, queryTokens, threshold);
        if (positions.isEmpty()) {
            return text;
        }

        StringBuilder highlighted = new StringBuilder();
        int currentIndex = 0;

        for (MatchPosition pos : positions) {
            // Append text before this match
            if (pos.getStart() > currentIndex) {
                highlighted.append(text, currentIndex, pos.getStart());
            }
            // Wrap matched text with <em> markers
            highlighted.append("<em>");
            highlighted.append(text, pos.getStart(), pos.getEnd());
            highlighted.append("</em>");
            currentIndex = pos.getEnd();
        }

        // Append any remaining text after the last match
        if (currentIndex < text.length()) {
            highlighted.append(text, currentIndex, text.length());
        }

        return highlighted.toString();
    }

    /**
     * Find all match positions in the text for the given query tokens.
     * Collects fuzzy matches for each token, merges overlapping positions,
     * and returns the result sorted by start position.
     *
     * @param text        the text to search within
     * @param queryTokens the search query tokens to match against
     * @param threshold   minimum similarity score (0.0-1.0) to consider a match
     * @return sorted list of non-overlapping match positions, or empty list if no matches
     */
    public List<MatchPosition> findMatchPositions(String text, List<String> queryTokens, double threshold) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        if (queryTokens == null || queryTokens.isEmpty()) {
            return new ArrayList<>();
        }

        List<MatchPosition> allPositions = new ArrayList<>();

        for (String token : queryTokens) {
            if (token == null || token.isEmpty()) {
                continue;
            }
            List<MatchPosition> tokenMatches = fuzzyMatchingService.findFuzzyMatches(text, token, threshold);
            allPositions.addAll(tokenMatches);
        }

        if (allPositions.isEmpty()) {
            return allPositions;
        }

        return mergeOverlappingPositions(allPositions, text);
    }

    /**
     * Merge overlapping or adjacent match positions into non-overlapping ranges.
     * Positions are sorted by start, then merged when they overlap or are adjacent.
     * The merged position keeps the highest similarity score from its constituents.
     *
     * @param positions the raw list of match positions (may overlap)
     * @param text      the original text (used to extract merged matched text)
     * @return sorted, non-overlapping list of merged match positions
     */
    private List<MatchPosition> mergeOverlappingPositions(List<MatchPosition> positions, String text) {
        // Sort by start position, then by end position descending for same start
        positions.sort(Comparator.comparingInt(MatchPosition::getStart)
                .thenComparing(Comparator.comparingInt(MatchPosition::getEnd).reversed()));

        List<MatchPosition> merged = new ArrayList<>();
        MatchPosition current = positions.get(0);
        int mergedStart = current.getStart();
        int mergedEnd = current.getEnd();
        double bestSimilarity = current.getSimilarity();

        for (int i = 1; i < positions.size(); i++) {
            MatchPosition next = positions.get(i);
            if (next.getStart() <= mergedEnd) {
                // Overlapping or adjacent — extend the range
                mergedEnd = Math.max(mergedEnd, next.getEnd());
                bestSimilarity = Math.max(bestSimilarity, next.getSimilarity());
            } else {
                // No overlap — finalize current merged position and start a new one
                String matchedText = text.substring(mergedStart, mergedEnd);
                merged.add(new MatchPosition(mergedStart, mergedEnd, bestSimilarity, matchedText));
                mergedStart = next.getStart();
                mergedEnd = next.getEnd();
                bestSimilarity = next.getSimilarity();
            }
        }

        // Add the last merged position
        String matchedText = text.substring(mergedStart, mergedEnd);
        merged.add(new MatchPosition(mergedStart, mergedEnd, bestSimilarity, matchedText));

        return merged;
    }
}
