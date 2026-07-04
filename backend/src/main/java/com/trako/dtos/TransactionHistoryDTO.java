package com.trako.dtos;

import java.util.Date;
import java.util.List;

/**
 * A history entry as returned by the API: enough metadata to render a timeline / recycle-bin row
 * (operation, when, and a preview of the affected transaction) and the {@code id} used to revert.
 *
 * <p>{@code changes} is populated only for UPDATE entries in a single transaction's timeline: it
 * lists the fields that edit altered (before → after), so the UI can show a diff. It is
 * {@code null} for other operations and for the global/recycle-bin listings.
 */
public record TransactionHistoryDTO(
        Long id,
        Long transactionId,
        String operation,
        Date changedAt,
        Long linkedTransactionId,
        String name,
        Double amount,
        String originalCurrency,
        Date date,
        Integer transactionType,
        List<TransactionFieldChangeDTO> changes
) {}
