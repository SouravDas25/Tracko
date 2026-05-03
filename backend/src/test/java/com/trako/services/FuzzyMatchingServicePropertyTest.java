package com.trako.services;

import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for FuzzyMatchingService.
 *
 * <p><b>Validates: Requirements 1.3</b> — fuzzy matching to handle minor spelling variations
 * and partial matches.</p>
 *
 * <p>Property 3: Fuzzy Matching Tolerance —
 * For any search query and transaction name with Levenshtein similarity >= the configured
 * fuzzy threshold, the transaction SHALL be included in search results.</p>
 */
public class FuzzyMatchingServicePropertyTest {

    private final FuzzyMatchingService service = new FuzzyMatchingService();

    // -----------------------------------------------------------------------
    // Property 3a: If calculateSimilarity(text, query) >= threshold,
    //              then fuzzyContains(text, query, threshold) must return true.
    // -----------------------------------------------------------------------

    /**
     * <b>Validates: Requirements 1.3</b>
     *
     * <p>For any non-empty text and query of equal length, when the Levenshtein similarity
     * between them meets or exceeds the threshold, fuzzyContains must report a match.
     * This directly validates Property 3 — fuzzy matching tolerance.</p>
     */
    @Property(tries = 500)
    void whenSimilarityMeetsThreshold_fuzzyContains_returnsTrue(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String text,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String query
    ) {
        double threshold = 0.6;
        double similarity = service.calculateSimilarity(text, query);

        if (similarity >= threshold) {
            boolean result = service.fuzzyContains(text, query, threshold);
            assertThat(result)
                    .as("fuzzyContains should return true when calculateSimilarity(text='%s', query='%s') = %.4f >= threshold %.1f",
                            text, query, similarity, threshold)
                    .isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // Property 3b: calculateSimilarity always returns a value in [0.0, 1.0].
    // -----------------------------------------------------------------------

    /**
     * <b>Validates: Requirements 1.3</b>
     *
     * <p>For any pair of non-null strings, the similarity score must be bounded
     * between 0.0 and 1.0 inclusive. This ensures the fuzzy matching threshold
     * comparison is well-defined.</p>
     */
    @Property(tries = 500)
    void similarityScore_isAlwaysBetweenZeroAndOne(
            @ForAll @AlphaChars @StringLength(min = 0, max = 30) String s1,
            @ForAll @AlphaChars @StringLength(min = 0, max = 30) String s2
    ) {
        double similarity = service.calculateSimilarity(s1, s2);

        assertThat(similarity)
                .as("Similarity between '%s' and '%s' should be in [0.0, 1.0]", s1, s2)
                .isBetween(0.0, 1.0);
    }

    // -----------------------------------------------------------------------
    // Property 3c: calculateSimilarity is symmetric — sim(a, b) == sim(b, a).
    // -----------------------------------------------------------------------

    /**
     * <b>Validates: Requirements 1.3</b>
     *
     * <p>Levenshtein distance is symmetric, so the similarity score derived from it
     * must also be symmetric. This ensures fuzzy matching behaves consistently
     * regardless of argument order.</p>
     */
    @Property(tries = 500)
    void similarityScore_isSymmetric(
            @ForAll @AlphaChars @StringLength(min = 0, max = 30) String a,
            @ForAll @AlphaChars @StringLength(min = 0, max = 30) String b
    ) {
        double simAB = service.calculateSimilarity(a, b);
        double simBA = service.calculateSimilarity(b, a);

        assertThat(simAB)
                .as("Similarity should be symmetric: sim('%s','%s')=%.4f should equal sim('%s','%s')=%.4f",
                        a, b, simAB, b, a, simBA)
                .isEqualTo(simBA);
    }
}
