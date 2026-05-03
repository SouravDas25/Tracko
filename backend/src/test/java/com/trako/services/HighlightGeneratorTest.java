package com.trako.services;

import com.trako.models.MatchPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HighlightGenerator.
 * Validates: Requirements 4.1 (highlight matching terms within transaction fields)
 */
@ExtendWith(MockitoExtension.class)
public class HighlightGeneratorTest {

    @Mock
    private FuzzyMatchingService fuzzyMatchingService;

    @InjectMocks
    private HighlightGenerator highlightGenerator;

    private static final double THRESHOLD = 0.6;

    // --- Single match highlighting ---

    @Test
    public void generateHighlights_singleMatch_wrapsMatchWithEmTags() {
        String text = "hello world";
        List<String> queryTokens = List.of("world");

        when(fuzzyMatchingService.findFuzzyMatches("hello world", "world", THRESHOLD))
                .thenReturn(List.of(new MatchPosition(6, 11, 1.0, "world")));

        String result = highlightGenerator.generateHighlights(text, queryTokens, THRESHOLD);

        assertThat(result).isEqualTo("hello <em>world</em>");
    }

    // --- Multiple matches in same field ---

    @Test
    public void generateHighlights_multipleMatches_wrapsEachMatchWithEmTags() {
        String text = "coffee and more coffee";
        List<String> queryTokens = List.of("coffee");

        when(fuzzyMatchingService.findFuzzyMatches("coffee and more coffee", "coffee", THRESHOLD))
                .thenReturn(List.of(
                        new MatchPosition(0, 6, 1.0, "coffee"),
                        new MatchPosition(16, 22, 1.0, "coffee")
                ));

        String result = highlightGenerator.generateHighlights(text, queryTokens, THRESHOLD);

        assertThat(result).isEqualTo("<em>coffee</em> and more <em>coffee</em>");
    }

    // --- No matches returns original text ---

    @Test
    public void generateHighlights_noMatches_returnsOriginalText() {
        String text = "hello world";
        List<String> queryTokens = List.of("zzzzz");

        when(fuzzyMatchingService.findFuzzyMatches("hello world", "zzzzz", THRESHOLD))
                .thenReturn(Collections.emptyList());

        String result = highlightGenerator.generateHighlights(text, queryTokens, THRESHOLD);

        assertThat(result).isEqualTo("hello world");
    }

    // --- Overlapping match handling (two tokens overlap in text, should be merged) ---

    @Test
    public void generateHighlights_overlappingMatches_mergesIntoSingleHighlight() {
        // Two query tokens produce overlapping match positions in the text
        String text = "coffeeshop";
        List<String> queryTokens = List.of("coffee", "shop");

        // "coffee" matches positions 0-6, "shop" matches positions 6-10
        // These are adjacent/overlapping and should be merged into one highlight
        when(fuzzyMatchingService.findFuzzyMatches("coffeeshop", "coffee", THRESHOLD))
                .thenReturn(List.of(new MatchPosition(0, 6, 1.0, "coffee")));
        when(fuzzyMatchingService.findFuzzyMatches("coffeeshop", "shop", THRESHOLD))
                .thenReturn(List.of(new MatchPosition(6, 10, 1.0, "shop")));

        String result = highlightGenerator.generateHighlights(text, queryTokens, THRESHOLD);

        assertThat(result).isEqualTo("<em>coffeeshop</em>");
    }

    // --- Null text returns null ---

    @Test
    public void generateHighlights_nullText_returnsNull() {
        String result = highlightGenerator.generateHighlights(null, List.of("query"), THRESHOLD);

        assertThat(result).isNull();
    }

    // --- Empty query tokens returns original text ---

    @Test
    public void generateHighlights_emptyQueryTokens_returnsOriginalText() {
        String result = highlightGenerator.generateHighlights("hello world", Collections.emptyList(), THRESHOLD);

        assertThat(result).isEqualTo("hello world");
    }

    @Test
    public void generateHighlights_nullQueryTokens_returnsOriginalText() {
        String result = highlightGenerator.generateHighlights("hello world", null, THRESHOLD);

        assertThat(result).isEqualTo("hello world");
    }

    // --- findMatchPositions returns correct positions ---

    @Test
    public void findMatchPositions_returnsCorrectPositions() {
        String text = "hello world";
        List<String> queryTokens = List.of("world");

        when(fuzzyMatchingService.findFuzzyMatches("hello world", "world", THRESHOLD))
                .thenReturn(List.of(new MatchPosition(6, 11, 1.0, "world")));

        List<MatchPosition> positions = highlightGenerator.findMatchPositions(text, queryTokens, THRESHOLD);

        assertThat(positions).hasSize(1);
        MatchPosition pos = positions.get(0);
        assertThat(pos.getStart()).isEqualTo(6);
        assertThat(pos.getEnd()).isEqualTo(11);
        assertThat(pos.getMatchedText()).isEqualTo("world");
    }

    @Test
    public void findMatchPositions_multipleTokens_returnsMergedPositions() {
        String text = "coffee and tea";
        List<String> queryTokens = List.of("coffee", "tea");

        when(fuzzyMatchingService.findFuzzyMatches("coffee and tea", "coffee", THRESHOLD))
                .thenReturn(List.of(new MatchPosition(0, 6, 1.0, "coffee")));
        when(fuzzyMatchingService.findFuzzyMatches("coffee and tea", "tea", THRESHOLD))
                .thenReturn(List.of(new MatchPosition(11, 14, 1.0, "tea")));

        List<MatchPosition> positions = highlightGenerator.findMatchPositions(text, queryTokens, THRESHOLD);

        assertThat(positions).hasSize(2);
        assertThat(positions.get(0).getStart()).isEqualTo(0);
        assertThat(positions.get(0).getEnd()).isEqualTo(6);
        assertThat(positions.get(1).getStart()).isEqualTo(11);
        assertThat(positions.get(1).getEnd()).isEqualTo(14);
    }

    @Test
    public void findMatchPositions_nullText_returnsEmptyList() {
        List<MatchPosition> positions = highlightGenerator.findMatchPositions(null, List.of("query"), THRESHOLD);

        assertThat(positions).isEmpty();
    }

    @Test
    public void findMatchPositions_emptyText_returnsEmptyList() {
        List<MatchPosition> positions = highlightGenerator.findMatchPositions("", List.of("query"), THRESHOLD);

        assertThat(positions).isEmpty();
    }
}
