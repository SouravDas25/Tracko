package com.trako.services;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for TransactionSearchServiceImpl query normalization and tokenization.
 *
 * <p><b>Validates: Requirements 6.1</b> — THE Search_Engine SHALL normalize search queries
 * (case-insensitive, whitespace trimming, special character handling).</p>
 *
 * <p>Property 7: Query Normalization Invariance —
 * For any search query, searching with different case variations or extra whitespace
 * SHALL produce identical results to the normalized query.</p>
 *
 * <p>Since TransactionSearchServiceImpl has many {@code @Autowired} dependencies,
 * these tests exercise only the pure {@code normalizeQuery} and {@code tokenizeQuery}
 * methods which have no external dependencies.</p>
 */
public class TransactionSearchServicePropertyTest {

    private final TransactionSearchServiceImpl service = new TransactionSearchServiceImpl();

    // -----------------------------------------------------------------------
    // Property 7a: normalizeQuery is idempotent —
    //              normalizeQuery(normalizeQuery(q)) == normalizeQuery(q)
    // -----------------------------------------------------------------------

    /**
     * <b>Validates: Requirements 6.1</b>
     *
     * <p>For any arbitrary string, applying normalizeQuery twice must produce the same
     * result as applying it once. This guarantees that normalized queries are already
     * in their canonical form and re-normalization is a no-op.</p>
     */
    @Property(tries = 500)
    void normalizeQuery_isIdempotent(
            @ForAll @StringLength(min = 0, max = 100) String query
    ) {
        String once = service.normalizeQuery(query);
        String twice = service.normalizeQuery(once);

        assertThat(twice)
                .as("normalizeQuery should be idempotent: normalizeQuery(normalizeQuery('%s')) should equal normalizeQuery('%s')",
                        query, query)
                .isEqualTo(once);
    }

    // -----------------------------------------------------------------------
    // Property 7b: tokenizeQuery produces same tokens regardless of case —
    //              tokenizeQuery("ABC DEF") == tokenizeQuery("abc def")
    // -----------------------------------------------------------------------

    /**
     * <b>Validates: Requirements 6.1</b>
     *
     * <p>For any ASCII string, tokenizing the uppercase version must yield the same tokens
     * as tokenizing the lowercase version. This ensures case-insensitive search
     * behaviour — users get identical results regardless of how they capitalize
     * their query.</p>
     *
     * <p>Note: We restrict to ASCII characters because some Unicode characters have
     * complex case mappings (e.g., Turkish dotless i) where toUpperCase().toLowerCase()
     * does not round-trip to the original character.</p>
     */
    @Property(tries = 500)
    void tokenizeQuery_isCaseInsensitive(
            @ForAll("asciiStrings") String query
    ) {
        List<String> tokensUpper = service.tokenizeQuery(query.toUpperCase());
        List<String> tokensLower = service.tokenizeQuery(query.toLowerCase());

        assertThat(tokensUpper)
                .as("tokenizeQuery should produce identical tokens for upper-case '%s' and lower-case '%s'",
                        query.toUpperCase(), query.toLowerCase())
                .isEqualTo(tokensLower);
    }

    // -----------------------------------------------------------------------
    // Property 7c: tokenizeQuery produces same tokens regardless of extra
    //              whitespace — tokenizeQuery("a  b") == tokenizeQuery("a b")
    // -----------------------------------------------------------------------

    /**
     * <b>Validates: Requirements 6.1</b>
     *
     * <p>For any two non-empty words, inserting extra whitespace between them must
     * not change the resulting tokens. This ensures that queries like
     * {@code "coffee  shop"} and {@code "coffee shop"} are treated identically.</p>
     */
    @Property(tries = 500)
    void tokenizeQuery_isWhitespaceInsensitive(
            @ForAll("nonBlankWords") String word1,
            @ForAll("nonBlankWords") String word2,
            @ForAll("extraWhitespace") String extraSpace
    ) {
        String singleSpace = word1 + " " + word2;
        String multiSpace = word1 + extraSpace + word2;

        List<String> tokensSingle = service.tokenizeQuery(singleSpace);
        List<String> tokensMulti = service.tokenizeQuery(multiSpace);

        assertThat(tokensMulti)
                .as("tokenizeQuery('%s') should equal tokenizeQuery('%s')", multiSpace, singleSpace)
                .isEqualTo(tokensSingle);
    }

    // -----------------------------------------------------------------------
    // Custom providers
    // -----------------------------------------------------------------------

    @Provide
    Arbitrary<String> asciiStrings() {
        // Generate strings with only ASCII letters, digits, and common punctuation
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', '-', '_', '.', ',')
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    @Provide
    Arbitrary<String> nonBlankWords() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(15);
    }

    @Provide
    Arbitrary<String> extraWhitespace() {
        // Generate whitespace strings of length 2–10 (always more than a single space)
        return Arbitraries.strings()
                .withChars(' ', '\t')
                .ofMinLength(2)
                .ofMaxLength(10);
    }
}
