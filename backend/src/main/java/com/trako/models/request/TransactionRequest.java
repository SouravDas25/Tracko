package com.trako.models.request;

import jakarta.validation.constraints.NotNull;
import java.util.Date;

/**
 * Unified request model for creating transactions and transfers.
 * 
 * <p>For regular transactions: Include accountId, categoryId, transactionType, amount, etc.
 * <p>For transfers: Include accountId (or fromAccountId), toAccountId, and amount.
 * The presence of toAccountId indicates this is a transfer request.
 */
public record TransactionRequest(
    // Common fields
    Long id,
    Long accountId,  // For transactions, this is the account. For transfers, this is the source (fromAccountId)
    Date date,
    Double amount,
    String name,
    String comments,
    
    // Regular transaction fields
    Long categoryId,
    Integer transactionType,  // 1=DEBIT, 2=CREDIT
    Integer isCountable,
    String originalCurrency,
    Double originalAmount,
    Double exchangeRate,
    Long linkedTransactionId,
    
    // Transfer-specific field
    Long toAccountId,  // If present, this is a TRANSFER request
    Long fromAccountId  // Alternative to accountId for transfers (for API clarity)
) {
    
    /**
     * Checks if this request represents a transfer (vs a regular transaction).
     */
    public boolean isTransfer() {
        return toAccountId != null;
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
