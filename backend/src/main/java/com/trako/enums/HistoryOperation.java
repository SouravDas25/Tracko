package com.trako.enums;

/**
 * The kind of change recorded in a {@code transaction_history} row.
 *
 * <ul>
 *   <li>{@code CREATE} — a transaction was created (audit only; undoing an add is a normal delete).</li>
 *   <li>{@code UPDATE} — snapshot of the state before an edit.</li>
 *   <li>{@code DELETE} — snapshot of the state before a delete (the recycle-bin view).</li>
 *   <li>{@code REVERT} — a previous snapshot was rolled back (audit of the revert itself).</li>
 * </ul>
 */
public enum HistoryOperation {
    CREATE,
    UPDATE,
    DELETE,
    REVERT
}
