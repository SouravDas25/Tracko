package com.trako.models.request;

import com.trako.entities.TransactionType;

import java.util.Date;

/**
 * Unified request model for creating transactions and transfers.
 *
 * <p>For regular transactions: Include accountId, categoryId, transactionType, originalAmount, originalCurrency, etc.
 * <p>For transfers: Set transactionType to TRANSFER, include accountId (or fromAccountId), toAccountId, originalAmount, and originalCurrency.
 * transactionType=TRANSFER is the signal that this is a transfer request; toAccountId is required in that case.
 */
public record TransactionRequest(
        // Common fields
        Long id,
        Long accountId,  // For transactions, this is the account. For transfers, this is the source (fromAccountId)
        Date date,
        String name,
        String comments,

        // Regular transaction fields
        Long categoryId,
        TransactionType transactionType,  // 1=DEBIT, 2=CREDIT
        Integer isCountable,
        String originalCurrency,
        Double originalAmount,
        Double exchangeRate,
        Long linkedTransactionId,

        // Transfer-specific field
        Long toAccountId,  // Required when transactionType=TRANSFER
        Long fromAccountId  // Alternative to accountId for transfers (for API clarity)
) {

    /**
     * Checks if this request represents a transfer (vs a regular transaction).
     * transactionType=TRANSFER is the canonical signal; toAccountId is required when this is true.
     */
    public boolean isTransfer() {
        return transactionType == TransactionType.TRANSFER;
    }

    /**
     * Gets the source account ID for transfers (supports both accountId and fromAccountId).
     */
    public Long getSourceAccountId() {
        if (fromAccountId != null) {
            return fromAccountId;
        }
        return accountId;
    }
}
