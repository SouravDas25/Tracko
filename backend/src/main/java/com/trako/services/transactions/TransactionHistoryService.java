package com.trako.services.transactions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.dtos.SplitSnapshot;
import com.trako.dtos.TransactionFieldChangeDTO;
import com.trako.dtos.TransactionHistoryDTO;
import com.trako.dtos.TransactionHistoryPageDTO;
import com.trako.dtos.TransactionSnapshot;
import com.trako.entities.Split;
import com.trako.entities.Transaction;
import com.trako.entities.TransactionHistory;
import com.trako.enums.HistoryOperation;
import com.trako.enums.TransactionDbType;
import com.trako.exceptions.NotFoundException;
import com.trako.repositories.SplitRepository;
import com.trako.repositories.TransactionHistoryRepository;
import com.trako.repositories.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Records a snapshot of a transaction before every change and rolls changes back on request.
 *
 * <p>This is the backing store for the "change history / recycle bin": a DELETE snapshot lets a
 * deleted transaction be restored, and an UPDATE snapshot lets a bad edit be rolled back. For
 * transfers, both linked sides are captured in a single entry so a revert restores both together.
 */
@Service
public class TransactionHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionHistoryService.class);

    @Autowired
    private TransactionHistoryRepository historyRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SplitRepository splitRepository;

    @Autowired
    private TransactionValidationService validationService;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    // ==================== Recording ====================

    /**
     * Records a history entry for a change. {@code unit} is the set of transactions affected by one
     * user action — one for a regular transaction, both linked sides for a transfer. The
     * {@code primary} transaction is the one the user acted on (used as the entry's {@code transaction_id}).
     */
    @Transactional
    public void record(String userId, HistoryOperation operation, Transaction primary, List<Transaction> unit) {
        List<TransactionSnapshot> snapshots = new ArrayList<>(unit.size());
        for (Transaction t : unit) {
            snapshots.add(toSnapshot(t));
        }

        TransactionHistory entry = new TransactionHistory();
        entry.setUserId(userId);
        entry.setTransactionId(primary.getId());
        entry.setOperation(operation.name());
        entry.setLinkedTransactionId(primary.getLinkedTransactionId());
        entry.setChangedAt(new Date());
        entry.setSnapshot(serialize(snapshots));
        historyRepository.save(entry);
    }

    private TransactionSnapshot toSnapshot(Transaction t) {
        List<SplitSnapshot> splits = splitRepository.findByTransactionId(t.getId()).stream()
                .map(s -> new SplitSnapshot(s.getUserId(), s.getAmount(), s.getContactId(), s.getSettledAt(), s.getIsSettled()))
                .collect(Collectors.toList());
        return new TransactionSnapshot(
                t.getId(),
                t.getTransactionType() != null ? t.getTransactionType().getValue() : null,
                t.getName(),
                t.getComments(),
                t.getDate(),
                t.getOriginalCurrency(),
                t.getOriginalAmount(),
                t.getExchangeRate(),
                t.getAccountId(),
                t.getCategoryId(),
                t.getIsCountable(),
                t.getLinkedTransactionId(),
                splits
        );
    }

    // ==================== Reading ====================

    /**
     * A transaction's change timeline (most recent first). Each UPDATE entry carries a field-level
     * diff of what that edit changed (before → after), including the transaction type, so the UI can
     * show more than just "updated". The "after" state of an edit is the state captured just before
     * the next change (or the current live transaction for the most recent edit).
     */
    public List<TransactionHistoryDTO> listForTransaction(String userId, Long transactionId, String operation) {
        List<TransactionHistory> entries = (operation == null)
                ? historyRepository.findByUserIdAndTransactionIdOrderByChangedAtDesc(userId, transactionId)
                : historyRepository.findByUserIdAndTransactionIdAndOperationOrderByChangedAtDesc(userId, transactionId, operation);

        // Parse each entry's primary snapshot once, and capture the live state (null if since deleted).
        List<TransactionSnapshot> primaries = new ArrayList<>(entries.size());
        for (TransactionHistory e : entries) {
            primaries.add(primarySnapshot(e));
        }
        TransactionSnapshot live = transactionRepository.findById(transactionId)
                .map(this::toSnapshot).orElse(null);

        List<TransactionHistoryDTO> result = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            TransactionHistory entry = entries.get(i);
            List<TransactionFieldChangeDTO> changes = null;
            if (HistoryOperation.UPDATE.name().equals(entry.getOperation())) {
                TransactionSnapshot before = primaries.get(i);
                TransactionSnapshot after = afterImage(i, entries, primaries, live);
                if (before != null && after != null) {
                    changes = diff(before, after);
                }
            }
            result.add(toDTO(entry, changes));
        }
        return result;
    }

    /**
     * The state the transaction held immediately after the edit at {@code index}. For the most recent
     * change that is the current live transaction; otherwise it is the "before" snapshot captured by
     * the next (newer) UPDATE/DELETE. Returns null when it can't be determined unambiguously (e.g. a
     * REVERT happened next), so the diff is simply omitted rather than shown wrong.
     */
    private TransactionSnapshot afterImage(int index, List<TransactionHistory> entries,
                                           List<TransactionSnapshot> primaries, TransactionSnapshot live) {
        if (index == 0) {
            return live;
        }
        TransactionHistory newer = entries.get(index - 1);
        String op = newer.getOperation();
        if (HistoryOperation.UPDATE.name().equals(op) || HistoryOperation.DELETE.name().equals(op)) {
            return primaries.get(index - 1);
        }
        return null;
    }

    /** The fields that differ between two versions of a transaction, formatted for display. */
    private List<TransactionFieldChangeDTO> diff(TransactionSnapshot before, TransactionSnapshot after) {
        List<TransactionFieldChangeDTO> changes = new ArrayList<>();
        addChange(changes, "Type", typeLabel(before.transactionType()), typeLabel(after.transactionType()));
        addChange(changes, "Name", before.name(), after.name());
        addChange(changes, "Amount", formatAmount(before.originalAmount()), formatAmount(after.originalAmount()));
        addChange(changes, "Currency", before.originalCurrency(), after.originalCurrency());
        addChange(changes, "Date", formatDate(before.date()), formatDate(after.date()));
        addChange(changes, "Comments", before.comments(), after.comments());
        addChange(changes, "Countable", countableLabel(before.isCountable()), countableLabel(after.isCountable()));
        return changes;
    }

    private void addChange(List<TransactionFieldChangeDTO> changes, String field, String before, String after) {
        if (!Objects.equals(before, after)) {
            changes.add(new TransactionFieldChangeDTO(field, before, after));
        }
    }

    private String typeLabel(Integer value) {
        if (value == null) {
            return null;
        }
        if (value == TransactionDbType.TRANSFER_RENDERING_VALUE) {
            return "TRANSFER";
        }
        try {
            return TransactionDbType.fromValue(value).name();
        } catch (IllegalArgumentException ex) {
            return String.valueOf(value);
        }
    }

    private String formatAmount(Double amount) {
        if (amount == null) {
            return null;
        }
        if (!amount.isInfinite() && amount == Math.floor(amount)) {
            return String.valueOf(amount.longValue());
        }
        return String.valueOf(amount);
    }

    private String formatDate(Date date) {
        return date == null ? null : new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String countableLabel(Integer isCountable) {
        if (isCountable == null) {
            return null;
        }
        return isCountable == 0 ? "No" : "Yes";
    }

    /**
     * A page of the full change history across all of the user's transactions (most recent first),
     * optionally filtered to a single {@code operation} (e.g. {@code DELETE} for the recycle bin).
     * {@code operation} must already be a valid, canonical {@link com.trako.enums.HistoryOperation}
     * name or {@code null} for all operations.
     */
    public TransactionHistoryPageDTO listAll(String userId, String operation, Pageable pageable) {
        Page<TransactionHistory> page = (operation == null)
                ? historyRepository.findByUserId(userId, pageable)
                : historyRepository.findByUserIdAndOperation(userId, operation, pageable);
        return TransactionHistoryPageDTO.from(page.map(this::toDTO));
    }

    /** The recycle-bin view: every deleted transaction for the user (most recent first). */
    public List<TransactionHistoryDTO> listDeleted(String userId) {
        return historyRepository.findByUserIdAndOperationOrderByChangedAtDesc(userId, HistoryOperation.DELETE.name())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private TransactionHistoryDTO toDTO(TransactionHistory entry) {
        return toDTO(entry, null);
    }

    private TransactionHistoryDTO toDTO(TransactionHistory entry, List<TransactionFieldChangeDTO> changes) {
        TransactionSnapshot primary = primarySnapshot(entry);
        Double amount = null;
        if (primary != null && primary.originalAmount() != null && primary.exchangeRate() != null) {
            amount = primary.originalAmount() * primary.exchangeRate();
        }
        return new TransactionHistoryDTO(
                entry.getId(),
                entry.getTransactionId(),
                entry.getOperation(),
                entry.getChangedAt(),
                entry.getLinkedTransactionId(),
                primary != null ? primary.name() : null,
                amount,
                primary != null ? primary.originalCurrency() : null,
                primary != null ? primary.date() : null,
                primary != null ? primary.transactionType() : null,
                changes
        );
    }

    // ==================== Revert ====================

    /**
     * Rolls back to the state captured in the given history entry: an UPDATE/DELETE of a transaction
     * (and both linked sides for a transfer) is undone. Keyed on the history id so a deleted
     * transaction — whose row no longer exists — is still addressable.
     */
    @Transactional
    public void revert(String userId, Long historyId) {
        TransactionHistory entry = historyRepository.findByIdAndUserId(historyId, userId)
                .orElseThrow(() -> new NotFoundException("History entry not found: " + historyId));

        // Flush any pending delete from earlier in this transaction and drop the first-level cache so
        // existence checks below see the true DB state.
        entityManager.flush();
        entityManager.clear();

        List<TransactionSnapshot> snapshots = parseSnapshots(entry.getSnapshot());
        for (TransactionSnapshot snapshot : snapshots) {
            applySnapshot(userId, snapshot);
        }

        // Audit the revert itself so the timeline reflects it.
        TransactionHistory audit = new TransactionHistory();
        audit.setUserId(userId);
        audit.setTransactionId(entry.getTransactionId());
        audit.setOperation(HistoryOperation.REVERT.name());
        audit.setLinkedTransactionId(entry.getLinkedTransactionId());
        audit.setChangedAt(new Date());
        audit.setSnapshot(entry.getSnapshot());
        historyRepository.save(audit);
    }

    private void applySnapshot(String userId, TransactionSnapshot snapshot) {
        // The account must still exist and belong to the user (it may have been deleted since).
        validationService.validateAccountOwnership(userId, snapshot.accountId());

        Optional<Transaction> existing = transactionRepository.findById(snapshot.id());
        if (existing.isPresent()) {
            // Roll back an edit: overwrite the live row. Splits are managed separately and are not
            // changed by a transaction edit, so they are left as-is.
            Transaction t = existing.get();
            applyFields(t, snapshot);
            transactionRepository.save(t);
        } else {
            // Restore a deleted transaction, preserving its id, then re-create its splits.
            transactionRepository.insertWithId(
                    snapshot.id(),
                    snapshot.transactionType(),
                    snapshot.name(),
                    snapshot.comments(),
                    snapshot.date(),
                    snapshot.accountId(),
                    snapshot.categoryId(),
                    snapshot.isCountable(),
                    snapshot.originalCurrency(),
                    snapshot.originalAmount(),
                    snapshot.exchangeRate(),
                    snapshot.linkedTransactionId()
            );
            if (snapshot.splits() != null) {
                for (SplitSnapshot ss : snapshot.splits()) {
                    Split split = new Split();
                    split.setTransactionId(snapshot.id());
                    split.setUserId(ss.userId());
                    split.setAmount(ss.amount());
                    split.setContactId(ss.contactId());
                    split.setSettledAt(ss.settledAt());
                    split.setIsSettled(ss.isSettled());
                    splitRepository.save(split);
                }
            }
        }
    }

    private void applyFields(Transaction t, TransactionSnapshot snapshot) {
        t.setTransactionType(snapshot.transactionType() != null ? TransactionDbType.fromValue(snapshot.transactionType()) : null);
        t.setName(snapshot.name());
        t.setComments(snapshot.comments());
        t.setDate(snapshot.date());
        t.setOriginalCurrency(snapshot.originalCurrency());
        t.setOriginalAmount(snapshot.originalAmount());
        t.setExchangeRate(snapshot.exchangeRate());
        t.setAccountId(snapshot.accountId());
        t.setCategoryId(snapshot.categoryId());
        t.setIsCountable(snapshot.isCountable());
        t.setLinkedTransactionId(snapshot.linkedTransactionId());
    }

    // ==================== Serialization helpers ====================

    private String serialize(List<TransactionSnapshot> snapshots) {
        try {
            return objectMapper.writeValueAsString(snapshots);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize transaction snapshot", e);
        }
    }

    private List<TransactionSnapshot> parseSnapshots(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<TransactionSnapshot>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to read transaction snapshot", e);
        }
    }

    private TransactionSnapshot primarySnapshot(TransactionHistory entry) {
        List<TransactionSnapshot> snapshots = parseSnapshots(entry.getSnapshot());
        if (snapshots.isEmpty()) {
            return null;
        }
        return snapshots.stream()
                .filter(s -> entry.getTransactionId().equals(s.id()))
                .findFirst()
                .orElse(snapshots.get(0));
    }
}
