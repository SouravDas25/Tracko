package com.trako.repositories;

import com.trako.entities.TransactionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

    /** A single transaction's change timeline, the most recent first. */
    List<TransactionHistory> findByUserIdAndTransactionIdOrderByChangedAtDesc(String userId, Long transactionId);

    /** A single transaction's change timeline filtered to one operation (e.g. DELETE for deletions). */
    List<TransactionHistory> findByUserIdAndTransactionIdAndOperationOrderByChangedAtDesc(String userId, Long transactionId, String operation);

    /** A page of the user's full change history across all transactions (ordering supplied by the Pageable). */
    Page<TransactionHistory> findByUserId(String userId, Pageable pageable);

    /** A page of the user's history filtered to one operation (e.g. DELETE for the recycle bin). */
    Page<TransactionHistory> findByUserIdAndOperation(String userId, String operation, Pageable pageable);

    /** The deleted/recycle-bin view: DELETE entries for the user, most recent first. */
    List<TransactionHistory> findByUserIdAndOperationOrderByChangedAtDesc(String userId, String operation);

    /** A history entry scoped to its owner — the basis for revert ownership checks. */
    Optional<TransactionHistory> findByIdAndUserId(Long id, String userId);
}
