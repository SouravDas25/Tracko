package com.trako.services;

import com.trako.dtos.*;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.models.ScoredTransaction;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransactionSearchServiceImpl implements TransactionSearchService {

    @Autowired
    private TransactionSearchRepository transactionSearchRepository;

    @Autowired
    private FuzzyMatchingService fuzzyMatchingService;

    @Autowired
    private RelevanceScorer relevanceScorer;

    @Autowired
    private HighlightGenerator highlightGenerator;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    @Override
    public List<String> tokenizeQuery(String query) {
        String normalized = normalizeQuery(query);
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(normalized.split(" "))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public TransactionSearchResultDTO search(String userId, SearchRequestDTO request) {
        long startTime = System.currentTimeMillis();

        // 1. Normalize and tokenize the query
        String normalizedQuery = normalizeQuery(request.getQuery());
        List<String> queryTokens = tokenizeQuery(request.getQuery());

        // 2. Build Pageable from request
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        Pageable pageable = PageRequest.of(page, size);

        // 3. Phase 1: Database LIKE query to get candidate set
        Page<Transaction> transactionPage = transactionSearchRepository.searchTransactions(
                userId,
                queryTokens,
                request.getStartDate(),
                request.getEndDate(),
                request.getMinAmount(),
                request.getMaxAmount(),
                request.getAccountIds(),
                request.getCategoryId(),
                pageable);

        List<Transaction> transactions = transactionPage.getContent();

        // 4. Batch fetch categories for all transactions
        Set<Long> categoryIds = transactions.stream()
                .map(Transaction::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Category> categoryMap = Collections.emptyMap();
        if (!categoryIds.isEmpty()) {
            categoryMap = categoryRepository.findAllById(categoryIds).stream()
                    .collect(Collectors.toMap(Category::getId, Function.identity()));
        }

        // 5. Batch fetch accounts for all transactions
        Set<Long> accountIds = transactions.stream()
                .map(Transaction::getAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Account> accountMap = Collections.emptyMap();
        if (!accountIds.isEmpty()) {
            accountMap = accountRepository.findAllById(accountIds).stream()
                    .collect(Collectors.toMap(Account::getId, Function.identity()));
        }

        // 6. Phase 2: Application-level fuzzy scoring and ranking
        double fuzzyThreshold = request.getFuzzyThreshold() != null ? request.getFuzzyThreshold() : 0.7;
        List<ScoredTransaction> scoredTransactions = relevanceScorer.rankByRelevance(
                transactions, categoryMap, queryTokens, fuzzyThreshold);

        // 7. Build search hit DTOs with highlights
        List<TransactionSearchHitDTO> results = new ArrayList<>();
        for (ScoredTransaction scored : scoredTransactions) {
            Transaction transaction = scored.getTransaction();
            Category category = scored.getCategory();
            Account account = accountMap.get(transaction.getAccountId());

            // Create TransactionDetailDTO
            TransactionDetailDTO detailDTO = new TransactionDetailDTO(
                    transaction, category, account, Collections.emptyList());

            // Generate highlights for matched fields
            Map<String, String> highlights = new LinkedHashMap<>();
            List<String> matchedFields = scored.getMatchedFields();

            if (matchedFields != null) {
                for (String field : matchedFields) {
                    String fieldValue = getFieldValue(transaction, category, field);
                    if (fieldValue != null && !fieldValue.isEmpty()) {
                        String highlighted = highlightGenerator.generateHighlights(
                                fieldValue, queryTokens, fuzzyThreshold);
                        highlights.put(field, highlighted);
                    }
                }
            }

            // Build the hit DTO
            TransactionSearchHitDTO hit = new TransactionSearchHitDTO();
            hit.setTransaction(detailDTO);
            hit.setRelevanceScore(scored.getRelevanceScore());
            hit.setHighlights(highlights);
            hit.setMatchedFields(matchedFields != null ? matchedFields : Collections.emptyList());

            results.add(hit);
        }

        // 8. Build result DTO with pagination metadata
        long totalResults = transactionPage.getTotalElements();
        int totalPages = (size > 0) ? (int) Math.ceil((double) totalResults / size) : 0;
        long searchTimeMs = System.currentTimeMillis() - startTime;

        TransactionSearchResultDTO resultDTO = new TransactionSearchResultDTO();
        resultDTO.setResults(results);
        resultDTO.setTotalResults(totalResults);
        resultDTO.setPage(page);
        resultDTO.setSize(size);
        resultDTO.setTotalPages(totalPages);
        resultDTO.setHasNext(page < totalPages - 1);
        resultDTO.setHasPrevious(page > 0);
        resultDTO.setSearchTimeMs(searchTimeMs);
        resultDTO.setQuery(request.getQuery());

        return resultDTO;
    }

    /**
     * Extract the string value of a field from a transaction for highlight generation.
     */
    private String getFieldValue(Transaction transaction, Category category, String fieldName) {
        if (transaction == null || fieldName == null) {
            return null;
        }
        switch (fieldName) {
            case "name":
                return transaction.getName();
            case "comments":
                return transaction.getComments();
            case "category":
                return (category != null) ? category.getName() : null;
            case "amount":
                return (transaction.getAmount() != null) ? transaction.getAmount().toString() : null;
            case "date":
                if (transaction.getDate() != null) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    return sdf.format(transaction.getDate());
                }
                return null;
            default:
                return null;
        }
    }
}
