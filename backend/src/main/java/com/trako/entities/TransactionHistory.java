package com.trako.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * A change-history / recycle-bin entry for a transaction.
 *
 * <p>One row is written before every UPDATE/DELETE (and after every CREATE) of a transaction. The
 * {@code snapshot} holds the JSON-serialized state of the affected transaction(s) — for a transfer,
 * both linked sides — so any change can be viewed and rolled back. There is intentionally no FK on
 * {@code transactionId}: a DELETE entry must survive the hard-deleted transaction row.
 */
@Entity
@Table(name = "transaction_history")
@Getter
@Setter
public class TransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "transaction_id")
    private Long transactionId;

    @NotNull
    @Column(name = "user_id", length = 36)
    private String userId;

    @NotNull
    @Column(name = "operation", length = 16)
    private String operation;

    @NotNull
    @Column(name = "snapshot")
    private String snapshot;

    @Column(name = "linked_transaction_id")
    private Long linkedTransactionId;

    @Column(name = "changed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date changedAt;
}
