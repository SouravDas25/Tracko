package com.trako.dtos;

import java.util.Date;

/**
 * Serialized state of a {@code Split} captured in a transaction-history snapshot, so a deleted
 * transaction's splits can be re-created on revert. The split id is intentionally omitted — new
 * rows are inserted on restore.
 */
public record SplitSnapshot(
        String userId,
        Double amount,
        Long contactId,
        Date settledAt,
        Integer isSettled
) {}
