package com.trako.services;

import com.trako.dtos.SearchRequestDTO;
import com.trako.dtos.TransactionSearchResultDTO;
import com.trako.entities.Transaction;
import com.trako.enums.TransactionDbType;
import com.trako.models.ScoredTransaction;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TransactionSearchServiceImpl.
 * Validates: Requirements 1.5 (empty result handling), 6.1 (query normalization)
 */
@ExtendWith(MockitoExtension.class)
public class TransactionSearchServiceTest {

    @Mock
    private TransactionSearchRepository transactionSearchRepository;

    @Mock
    private FuzzyMatchingService fuzzyMatchingService;

    @Mock
    private RelevanceScorer relevanceScorer;

    @Mock
    private HighlightGenerator highlightGenerator;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionSearchServiceImpl transactionSearchService;

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

    private SearchRequestDTO createSearchRequest(String query, int page, int size) {
        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery(query);
        request.setPage(page);
        request.setSize(size);
        return request;
    }

    // --- normalizeQuery tests ---

    @Test
    public void normalizeQuery_convertsToLowercaseAndTrimsAndCollapsesWhitespace() {
        assertThat(transactionSearchService.normalizeQuery("  HELLO   World  "))
                .isEqualTo("hello world");
    }

    @Test
    public void normalizeQuery_nullReturnsEmptyString() {
        assertThat(transactionSearchService.normalizeQuery(null))
                .isEqualTo("");
    }

    @Test
    public void normalizeQuery_alreadyNormalizedStringUnchanged() {
        assertThat(transactionSearchService.normalizeQuery("coffee shop"))
                .isEqualTo("coffee shop");
    }

    // --- tokenizeQuery tests ---

    @Test
    public void tokenizeQuery_splitsOnWhitespace() {
        List<String> tokens = transactionSearchService.tokenizeQuery("coffee shop visit");
        assertThat(tokens).containsExactly("coffee", "shop", "visit");
    }

    @Test
    public void tokenizeQuery_handlesExtraWhitespace() {
        List<String> tokens = transactionSearchService.tokenizeQuery("  coffee   shop  ");
        assertThat(tokens).containsExactly("coffee", "shop");
    }

    @Test
    public void tokenizeQuery_emptyStringReturnsEmptyList() {
        List<String> tokens = transactionSearchService.tokenizeQuery("");
        assertThat(tokens).isEmpty();
    }

    @Test
    public void tokenizeQuery_nullReturnsEmptyList() {
        List<String> tokens = transactionSearchService.tokenizeQuery(null);
        assertThat(tokens).isEmpty();
    }

    // --- search: empty results ---

    @Test
    public void search_emptyResults_returnsDtoWithZeroTotalAndEmptyList() {
        SearchRequestDTO request = createSearchRequest("nonexistent", 0, 20);

        Page<Transaction> emptyPage = new PageImpl<>(
                Collections.emptyList(), Pageable.ofSize(20), 0);

        when(transactionSearchRepository.searchTransactions(
                eq("user1"), anyList(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        when(relevanceScorer.rankByRelevance(
                eq(Collections.emptyList()), anyMap(), anyList(), anyDouble()))
                .thenReturn(Collections.emptyList());

        TransactionSearchResultDTO result = transactionSearchService.search("user1", request);

        assertThat(result.getTotalResults()).isEqualTo(0L);
        assertThat(result.getResults()).isEmpty();
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalPages()).isEqualTo(0);
        assertThat(result.getHasNext()).isFalse();
        assertThat(result.getHasPrevious()).isFalse();
        assertThat(result.getQuery()).isEqualTo("nonexistent");
        assertThat(result.getSearchTimeMs()).isNotNull();
    }

    // --- search: pagination metadata ---

    @Test
    public void search_paginationMetadata_calculatedCorrectly_firstPage() {
        SearchRequestDTO request = createSearchRequest("coffee", 0, 10);

        Transaction tx = createTransaction(1L, "coffee", null, 5.0);
        List<Transaction> transactions = List.of(tx);

        // 25 total results, page size 10 -> 3 total pages
        Page<Transaction> page = new PageImpl<>(transactions, Pageable.ofSize(10), 25);

        when(transactionSearchRepository.searchTransactions(
                eq("user1"), anyList(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        when(categoryRepository.findAllById(anySet())).thenReturn(Collections.emptyList());
        when(accountRepository.findAllById(anySet())).thenReturn(Collections.emptyList());

        ScoredTransaction scored = new ScoredTransaction(
                tx, null, 1.0, List.of("name"), Collections.emptyMap());
        when(relevanceScorer.rankByRelevance(anyList(), anyMap(), anyList(), anyDouble()))
                .thenReturn(List.of(scored));

        when(highlightGenerator.generateHighlights(anyString(), anyList(), anyDouble()))
                .thenReturn("<em>coffee</em>");

        TransactionSearchResultDTO result = transactionSearchService.search("user1", request);

        assertThat(result.getTotalResults()).isEqualTo(25L);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getHasNext()).isTrue();
        assertThat(result.getHasPrevious()).isFalse();
    }

    @Test
    public void search_paginationMetadata_calculatedCorrectly_middlePage() {
        SearchRequestDTO request = createSearchRequest("coffee", 1, 10);

        Transaction tx = createTransaction(1L, "coffee", null, 5.0);
        List<Transaction> transactions = List.of(tx);

        Page<Transaction> page = new PageImpl<>(transactions, Pageable.ofSize(10), 25);

        when(transactionSearchRepository.searchTransactions(
                eq("user1"), anyList(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        when(categoryRepository.findAllById(anySet())).thenReturn(Collections.emptyList());
        when(accountRepository.findAllById(anySet())).thenReturn(Collections.emptyList());

        ScoredTransaction scored = new ScoredTransaction(
                tx, null, 1.0, List.of("name"), Collections.emptyMap());
        when(relevanceScorer.rankByRelevance(anyList(), anyMap(), anyList(), anyDouble()))
                .thenReturn(List.of(scored));

        when(highlightGenerator.generateHighlights(anyString(), anyList(), anyDouble()))
                .thenReturn("<em>coffee</em>");

        TransactionSearchResultDTO result = transactionSearchService.search("user1", request);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getHasNext()).isTrue();
        assertThat(result.getHasPrevious()).isTrue();
    }

    @Test
    public void search_paginationMetadata_calculatedCorrectly_lastPage() {
        SearchRequestDTO request = createSearchRequest("coffee", 2, 10);

        Transaction tx = createTransaction(1L, "coffee", null, 5.0);
        List<Transaction> transactions = List.of(tx);

        Page<Transaction> page = new PageImpl<>(transactions, Pageable.ofSize(10), 25);

        when(transactionSearchRepository.searchTransactions(
                eq("user1"), anyList(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        when(categoryRepository.findAllById(anySet())).thenReturn(Collections.emptyList());
        when(accountRepository.findAllById(anySet())).thenReturn(Collections.emptyList());

        ScoredTransaction scored = new ScoredTransaction(
                tx, null, 1.0, List.of("name"), Collections.emptyMap());
        when(relevanceScorer.rankByRelevance(anyList(), anyMap(), anyList(), anyDouble()))
                .thenReturn(List.of(scored));

        when(highlightGenerator.generateHighlights(anyString(), anyList(), anyDouble()))
                .thenReturn("<em>coffee</em>");

        TransactionSearchResultDTO result = transactionSearchService.search("user1", request);

        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getHasNext()).isFalse();
        assertThat(result.getHasPrevious()).isTrue();
    }
}
