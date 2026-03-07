package com.trako.dtos;

import com.trako.entities.Transaction;

/**
 * Result object for transfer operations containing both sides of a transfer.
 *
 * @param debit  the debit side transaction (money leaving source account)
 * @param credit the credit side transaction (money entering a destination account)
 */
public record TransferResult(Transaction debit, Transaction credit) {

    /**
     * Gets the transaction matching the given ID.
     * Useful when updating a transfer and returning the side that was originally requested.
     */
    public Transaction getById(Long id) {
        if (debit.getId().equals(id)) {
            return debit;
        }
        if (credit.getId().equals(id)) {
            return credit;
        }
        throw new IllegalArgumentException("No transaction with id " + id + " found in this transfer");
    }
}
