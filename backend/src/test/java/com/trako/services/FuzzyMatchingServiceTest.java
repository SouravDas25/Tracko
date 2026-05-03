package com.trako.services;

import com.trako.models.MatchPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for FuzzyMatchingService.
 * Validates: Requirements 1.3 (fuzzy matching for minor spelling variations and partial matches)
 */
public class FuzzyMatchingServiceTest {

    private FuzzyMatchingService fuzzyMatchingService;

    @BeforeEach
    public void setup() {
        fuzzyMatchingService = new FuzzyMatchingService();
    }

    // --- calculateSimilarity tests ---

    @Test
    public void calculateSimilarity_exactMatch_returnsOne() {
        double similarity = fuzzyMatchingService.calculateSimilarity("hello", "hello");
        assertThat(similarity).isEqualTo(1.0);
    }

    @Test
    public void calculateSimilarity_completelyDifferentStrings_returnsLowScore() {
        double similarity = fuzzyMatchingService.calculateSimilarity("abc", "xyz");
        assertThat(similarity).isLessThan(0.5);
    }

    @Test
    public void calculateSimilarity_caseInsensitive_returnsOne() {
        double similarity = fuzzyMatchingService.calculateSimilarity("Hello", "hello");
        assertThat(similarity).isEqualTo(1.0);
    }

    @Test
    public void calculateSimilarity_nullFirstInput_returnsZero() {
        double similarity = fuzzyMatchingService.calculateSimilarity(null, "hello");
        assertThat(similarity).isEqualTo(0.0);
    }

    @Test
    public void calculateSimilarity_nullSecondInput_returnsZero() {
        double similarity = fuzzyMatchingService.calculateSimilarity("hello", null);
        assertThat(similarity).isEqualTo(0.0);
    }

    @Test
    public void calculateSimilarity_bothEmpty_returnsOne() {
        double similarity = fuzzyMatchingService.calculateSimilarity("", "");
        assertThat(similarity).isEqualTo(1.0);
    }

    @Test
    public void calculateSimilarity_oneEmpty_returnsZero() {
        double similarity = fuzzyMatchingService.calculateSimilarity("hello", "");
        assertThat(similarity).isEqualTo(0.0);
    }

    // --- fuzzyContains tests ---

    @Test
    public void fuzzyContains_exactSubstring_returnsTrue() {
        boolean result = fuzzyMatchingService.fuzzyContains("hello world", "world", 0.8);
        assertThat(result).isTrue();
    }

    @Test
    public void fuzzyContains_fuzzyMatchWithinThreshold_returnsTrue() {
        // "wrold" is one transposition away from "world" — similarity should be >= 0.6
        boolean result = fuzzyMatchingService.fuzzyContains("hello world", "wrold", 0.6);
        assertThat(result).isTrue();
    }

    @Test
    public void fuzzyContains_belowThreshold_returnsFalse() {
        boolean result = fuzzyMatchingService.fuzzyContains("hello world", "zzzzz", 0.8);
        assertThat(result).isFalse();
    }

    @Test
    public void fuzzyContains_nullText_returnsFalse() {
        boolean result = fuzzyMatchingService.fuzzyContains(null, "query", 0.6);
        assertThat(result).isFalse();
    }

    @Test
    public void fuzzyContains_nullQuery_returnsFalse() {
        boolean result = fuzzyMatchingService.fuzzyContains("text", null, 0.6);
        assertThat(result).isFalse();
    }

    @Test
    public void fuzzyContains_emptyText_returnsFalse() {
        boolean result = fuzzyMatchingService.fuzzyContains("", "query", 0.6);
        assertThat(result).isFalse();
    }

    @Test
    public void fuzzyContains_emptyQuery_returnsFalse() {
        boolean result = fuzzyMatchingService.fuzzyContains("text", "", 0.6);
        assertThat(result).isFalse();
    }

    // --- findFuzzyMatches tests ---

    @Test
    public void findFuzzyMatches_returnsCorrectMatchPositions() {
        List<MatchPosition> matches = fuzzyMatchingService.findFuzzyMatches(
                "hello world", "world", 0.8);

        assertThat(matches).hasSize(1);
        MatchPosition match = matches.get(0);
        assertThat(match.getStart()).isEqualTo(6);
        assertThat(match.getEnd()).isEqualTo(11);
        assertThat(match.getSimilarity()).isCloseTo(1.0, within(0.01));
        assertThat(match.getMatchedText()).isEqualTo("world");
    }

    @Test
    public void findFuzzyMatches_noMatches_returnsEmptyList() {
        List<MatchPosition> matches = fuzzyMatchingService.findFuzzyMatches(
                "hello world", "zzzzz", 0.8);

        assertThat(matches).isEmpty();
    }

    @Test
    public void findFuzzyMatches_nullInputs_returnsEmptyList() {
        assertThat(fuzzyMatchingService.findFuzzyMatches(null, "query", 0.8)).isEmpty();
        assertThat(fuzzyMatchingService.findFuzzyMatches("text", null, 0.8)).isEmpty();
        assertThat(fuzzyMatchingService.findFuzzyMatches("", "query", 0.8)).isEmpty();
        assertThat(fuzzyMatchingService.findFuzzyMatches("text", "", 0.8)).isEmpty();
    }
}
