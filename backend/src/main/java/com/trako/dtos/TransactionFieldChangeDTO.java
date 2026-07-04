package com.trako.dtos;

/**
 * A single before → after field change between two versions of a transaction, used to render the
 * diff for an edit in the transaction's change history (e.g. {@code Type: DEBIT → CREDIT}).
 * Values are already formatted for display; {@code null} means the field was unset in that version.
 */
public record TransactionFieldChangeDTO(String field, String before, String after) {}
