package com.trako.repositories;

import com.trako.entities.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;

/**
 * Custom search repository for transactions using JPA Criteria API.
 * Supports free-text search with LIKE queries, date/amount range filtering,
 * account and category filtering, and user authorization via account ownership.
 */
public interface TransactionSearchRepository {

    /**
     * Search transactions matching the given criteria.
     * Results are scoped to the specified user's accounts for authorization.
     * Text search uses LIKE queries across name and comments fields.
     *
     * @param userId       the authenticated user's ID (for authorization filtering)
     * @param queryTokens  normalized search tokens for LIKE matching
     * @param startDate    optional inclusive start date filter
     * @param endDate      optional exclusive end date filter
     * @param minAmount    optional minimum amount filter (inclusive)
     * @param maxAmount    optional maximum amount filter (inclusive)
     * @param accountIds   optional list of account IDs to restrict search
     * @param categoryId   optional category ID filter
     * @param pageable     pagination and sorting parameters
     * @return a page of matching transactions
     */
    Page<Transaction> searchTransactions(
            String userId,
            List<String> queryTokens,
            Date startDate,
            Date endDate,
            Double minAmount,
            Double maxAmount,
            List<Long> accountIds,
            Long categoryId,
            Pageable pageable);

    /**
     * Count total matching transactions for the given search criteria.
     * Uses the same filtering logic as {@link #searchTransactions} without pagination.
     *
     * @param userId       the authenticated user's ID (for authorization filtering)
     * @param queryTokens  normalized search tokens for LIKE matching
     * @param startDate    optional inclusive start date filter
     * @param endDate      optional exclusive end date filter
     * @param minAmount    optional minimum amount filter (inclusive)
     * @param maxAmount    optional maximum amount filter (inclusive)
     * @param accountIds   optional list of account IDs to restrict search
     * @param categoryId   optional category ID filter
     * @return the total count of matching transactions
     */
    long countSearchResults(
            String userId,
            List<String> queryTokens,
            Date startDate,
            Date endDate,
            Double minAmount,
            Double maxAmount,
            List<Long> accountIds,
            Long categoryId);
}
