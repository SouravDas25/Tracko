package com.trako.repositories;

import com.trako.entities.Account;
import com.trako.entities.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * JPA Criteria API implementation of TransactionSearchRepository.
 * Builds dynamic queries for free-text search across transaction fields
 * with user authorization, date/amount range filtering, and pagination.
 */
@Repository
public class TransactionSearchRepositoryImpl implements TransactionSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Transaction> searchTransactions(
            String userId,
            List<String> queryTokens,
            Date startDate,
            Date endDate,
            Double minAmount,
            Double maxAmount,
            List<Long> accountIds,
            Long categoryId,
            Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Transaction> query = cb.createQuery(Transaction.class);
        Root<Transaction> transaction = query.from(Transaction.class);

        List<Predicate> predicates = buildPredicates(
                cb, query, transaction, userId, queryTokens,
                startDate, endDate, minAmount, maxAmount, accountIds, categoryId);

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(transaction.get("date")), cb.desc(transaction.get("id")));

        TypedQuery<Transaction> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Transaction> results = typedQuery.getResultList();
        long total = countSearchResults(userId, queryTokens, startDate, endDate,
                minAmount, maxAmount, accountIds, categoryId);

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public long countSearchResults(
            String userId,
            List<String> queryTokens,
            Date startDate,
            Date endDate,
            Double minAmount,
            Double maxAmount,
            List<Long> accountIds,
            Long categoryId) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Transaction> transaction = countQuery.from(Transaction.class);

        List<Predicate> predicates = buildPredicates(
                cb, countQuery, transaction, userId, queryTokens,
                startDate, endDate, minAmount, maxAmount, accountIds, categoryId);

        countQuery.select(cb.count(transaction));
        countQuery.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    /**
     * Builds the shared list of predicates used by both search and count queries.
     * Includes user authorization, text search, date/amount ranges, and optional filters.
     */
    private List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<Transaction> transaction,
            String userId,
            List<String> queryTokens,
            Date startDate,
            Date endDate,
            Double minAmount,
            Double maxAmount,
            List<Long> accountIds,
            Long categoryId) {

        List<Predicate> predicates = new ArrayList<>();

        // User authorization: restrict to accounts owned by the user
        Subquery<Long> accountSubquery = query.subquery(Long.class);
        Root<Account> accountRoot = accountSubquery.from(Account.class);
        accountSubquery.select(accountRoot.get("id"))
                .where(cb.equal(accountRoot.get("userId"), userId));
        predicates.add(transaction.get("accountId").in(accountSubquery));

        // Text search: for each token, create OR predicates with LIKE on name, comments, and amount
        if (queryTokens != null && !queryTokens.isEmpty()) {
            List<Predicate> tokenPredicates = new ArrayList<>();
            for (String token : queryTokens) {
                String likePattern = "%" + token.toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(transaction.get("name")), likePattern);
                Predicate commentsLike = cb.like(cb.lower(transaction.get("comments")), likePattern);

                // Build list of OR predicates for this token
                List<Predicate> orPredicates = new ArrayList<>();
                orPredicates.add(nameLike);
                orPredicates.add(commentsLike);

                // If token is a valid number, also match against amount field
                try {
                    Double amountValue = Double.parseDouble(token);
                    Predicate amountEquals = cb.equal(transaction.get("amount"), amountValue);
                    orPredicates.add(amountEquals);
                } catch (NumberFormatException ignored) {
                    // Token is not a number, skip amount matching
                }

                tokenPredicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }
            predicates.add(cb.and(tokenPredicates.toArray(new Predicate[0])));
        }

        // Date range filter (startDate inclusive, endDate exclusive)
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(transaction.get("date"), startDate));
        }
        if (endDate != null) {
            predicates.add(cb.lessThan(transaction.get("date"), endDate));
        }

        // Amount range filter (both inclusive)
        if (minAmount != null) {
            predicates.add(cb.greaterThanOrEqualTo(transaction.get("amount"), minAmount));
        }
        if (maxAmount != null) {
            predicates.add(cb.lessThanOrEqualTo(transaction.get("amount"), maxAmount));
        }

        // Optional account IDs filter
        if (accountIds != null && !accountIds.isEmpty()) {
            predicates.add(transaction.get("accountId").in(accountIds));
        }

        // Optional category ID filter
        if (categoryId != null) {
            predicates.add(cb.equal(transaction.get("categoryId"), categoryId));
        }

        return predicates;
    }
}
