package com.trako.dtos;

import java.util.Date;
import java.util.List;

/**
 * Serialized state of a {@code Transaction} captured in a history entry. Holds every persistent
 * field except the DB-generated {@code amount}, plus the transaction's splits, so the transaction
 * can be fully rolled back (edit) or re-created (delete). {@code transactionType} is stored as its
 * integer {@code TransactionDbType} value.
 */
public record TransactionSnapshot(
        Long id,
        Integer transactionType,
        String name,
        String comments,
        Date date,
        String originalCurrency,
        Double originalAmount,
        Double exchangeRate,
        Long accountId,
        Long categoryId,
        Integer isCountable,
        Long linkedTransactionId,
        List<SplitSnapshot> splits
) {}
