package com.trako.services;

import com.trako.models.MatchPosition;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for HighlightGenerator.
 *
 * <p><b>Validates: Requirements 4.1, 4.2, 4.3</b></p>
 *
 * <p>Property 6: Search Result Metadata Accuracy —
 * For any search result, verify highlights identify correct match positions
 * and matched fields list is accurate.</p>
 */
public class HighlightGeneratorPropertyTest {

    private final HighlightGenerator generator;
    private final FuzzyMatchingService fuzzyService;

    public HighlightGeneratorPropertyTest() {
        generator = new HighlightGenerator();
        fuzzyService = new FuzzyMatchingService();
        try {
            Field field = HighlightGenerator.class.getDeclaredField("fuzzyMatchingService");
            field.setAccessible(true);
            field.set(generator, fuzzyService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject FuzzyMatchingService into HighlightGenerator", e);
        }
    }

    // -----------------------------------------------------------------------
    // Property 6a: For any text containing a query token as an exact
    //              substring, generateHighlights must include <em> tags
    //              around the match.
    // -----------------------------------------------------------------------

    /**
     * <b>Validates: Requirements 4.1, 4.2, 4.3</b>
     *
     * <p>For any text that contains a query token as an exact substring,
     * the highlighted output must wrap at least one occurrence of that token
     * with {@code <em>} tags. This validates that highlights correctly
     * identify match positions.</p>
     */
    @Property(tries = 500)
    void exactSubstringMatch_producesEmHighlight(
            @ForAll @AlphaChars @StringLength(min = 1, max = 10) String prefix,
            @ForAll @AlphaChars @StringLength(min = 2, max = 8) String token,
            @ForAll @AlphaChars @StringLength(min = 1, max = 10) String suffix
    ) {
        // Build text that is guaranteed to contain the token as a substring
        String text = prefix + token + suffix;
        List<String> queryTokens = List.of(token.toLowerCase());
        double threshold = 1.0; // exact match only

        String highlighted = generator.generateHighlights(text, queryTokens, threshold);

        assertThat(highlighted)
                .as("Highlighted text should contain <em> tags when text '%s' contains token '%s'",
                        text, token)
                .contains("<em>");
        assertThat(highlighted)
                .as("Highlighted text should contain </em> tags when text '%s' contains token '%s'",
                        text, token)
                .contains("</em>");
    }

    // -----------------------------------------------------------------------
    // Property 6b: For any text and query tokens, findMatchPositions returns
    //              positions where start < end and both are within text bounds.
    // -----------------------------------------------------------------------

    /**
     * <b>Validates: Requirements 4.1, 4.2, 4.3</b>
     *
     * <p>For any text and query tokens, every match position returned by
     * findMatchPositions must have start &lt; end, start &ge; 0, and
     * end &le; text.length(). This validates that match position metadata
     * is structurally correct.</p>
     */
    @Property(tries = 500)
    void matchPositions_haveValidBounds(
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String text,
            @ForAll @AlphaChars @StringLength(min = 1, max = 10) String queryToken
    ) {
        List<String> queryTokens = List.of(queryToken.toLowerCase());
        double threshold = 0.6;

        List<MatchPosition> positions = generator.findMatchPositions(text, queryTokens, threshold);

        for (MatchPosition pos : positions) {
            assertThat(pos.getStart())
                    .as("Match start (%d) should be >= 0 for text '%s', token '%s'",
                            pos.getStart(), text, queryToken)
                    .isGreaterThanOrEqualTo(0);

            assertThat(pos.getEnd())
                    .as("Match end (%d) should be <= text length (%d) for text '%s', token '%s'",
                            pos.getEnd(), text.length(), text, queryToken)
                    .isLessThanOrEqualTo(text.length());

            assertThat(pos.getStart())
                    .as("Match start (%d) should be < end (%d) for text '%s', token '%s'",
                            pos.getStart(), pos.getEnd(), text, queryToken)
                    .isLessThan(pos.getEnd());
        }
    }

    // -----------------------------------------------------------------------
    // Property 6c: For any text and query tokens, the highlighted text
    //              (with <em> tags removed) equals the original text.
    // -----------------------------------------------------------------------

    /**
     * <b>Validates: Requirements 4.1, 4.2, 4.3</b>
     *
     * <p>For any text and query tokens, stripping the {@code <em>} and
     * {@code </em>} markers from the highlighted output must yield the
     * original text. This validates that highlighting only adds markers
     * and never alters the underlying content.</p>
     */
    @Property(tries = 500)
    void highlightedText_strippedOfTags_equalsOriginal(
            @ForAll @AlphaChars @StringLength(min = 1, max = 30) String text,
            @ForAll @AlphaChars @StringLength(min = 1, max = 10) String queryToken
    ) {
        List<String> queryTokens = List.of(queryToken.toLowerCase());
        double threshold = 0.6;

        String highlighted = generator.generateHighlights(text, queryTokens, threshold);

        String stripped = highlighted
                .replace("<em>", "")
                .replace("</em>", "");

        assertThat(stripped)
                .as("Highlighted text with tags removed should equal original text '%s' (got '%s' from highlighted '%s')",
                        text, stripped, highlighted)
                .isEqualTo(text);
    }
}
