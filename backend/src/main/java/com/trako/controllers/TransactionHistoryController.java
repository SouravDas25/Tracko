package com.trako.controllers;

import com.trako.dtos.TransactionHistoryDTO;
import com.trako.dtos.TransactionHistoryPageDTO;
import com.trako.enums.HistoryOperation;
import com.trako.services.UserService;
import com.trako.services.transactions.TransactionHistoryService;
import com.trako.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Change history, recycle bin and revert for transactions.
 *
 * <p>Shares the {@code /api/transactions} base path with {@link TransactionController} but owns
 * every history-related endpoint. The global {@code /history} list is paged because it spans all of
 * a user's transactions and can grow without bound; the single-transaction timeline and the
 * recycle bin remain simple lists (a transaction's own timeline is naturally small).
 */
@Tag(name = "Transaction History", description = "Change history, recycle bin and revert for transactions")
@RestController
@RequestMapping("/api/transactions")
@Validated
public class TransactionHistoryController {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionHistoryService transactionHistoryService;

    /**
     * GET /api/transactions/history
     * A page of the user's full change history across all transactions (most recent first).
     * The optional {@code operation} filter narrows to one kind of change — {@code DELETE} yields
     * the recycle-bin view.
     */
    @Operation(summary = "List transaction change history (paged) across all transactions")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TransactionHistoryPageDTO.class)))
    @GetMapping("/history")
    public ResponseEntity<?> getAllHistory(
            @RequestParam(required = false) String operation,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "30") Integer size) {
        if (page == null || page < 0) {
            return Response.badRequest("page must be 0 or greater");
        }
        if (size == null || size < 1 || size > 200) {
            return Response.badRequest("size must be between 1 and 200");
        }
        String normalizedOperation = normalizeOperation(operation);
        if (operation != null && !operation.isBlank() && normalizedOperation == null) {
            return Response.badRequest("Invalid operation filter: " + operation);
        }

        String currentUserId = userService.loggedInUser().getId();
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "changedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return Response.ok(transactionHistoryService.listAll(currentUserId, normalizedOperation, pageable));
    }

    /** Maps a case-insensitive operation param to its canonical enum name, or null when absent/unknown. */
    private String normalizeOperation(String operation) {
        if (operation == null || operation.isBlank()) {
            return null;
        }
        for (HistoryOperation op : HistoryOperation.values()) {
            if (op.name().equalsIgnoreCase(operation)) {
                return op.name();
            }
        }
        return null;
    }

    /**
     * GET /api/transactions/trash
     * The recycle bin: the user's deleted transactions, most recent first. Each entry can be
     * restored via {@code POST /api/transactions/history/{historyId}/revert}.
     */
    @Operation(summary = "List deleted transactions (recycle bin)")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionHistoryDTO.class))))
    @GetMapping("/trash")
    public ResponseEntity<?> getTrash() {
        String currentUserId = userService.loggedInUser().getId();
        return Response.ok(transactionHistoryService.listDeleted(currentUserId));
    }

    /**
     * GET /api/transactions/{id}/history
     * The change timeline for a single transaction (create/edit/delete/revert), most recent first.
     */
    @Operation(summary = "Get a transaction's change history")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionHistoryDTO.class))))
    @GetMapping("/{id}/history")
    public ResponseEntity<?> getHistory(@PathVariable @Positive Long id) {
        String currentUserId = userService.loggedInUser().getId();
        return Response.ok(transactionHistoryService.listForTransaction(currentUserId, id));
    }

    /**
     * POST /api/transactions/history/{historyId}/revert
     * Rolls back to the state captured in the given history entry: undoes an edit, or restores a
     * deleted transaction (both linked sides for a transfer). Keyed on the history id so a deleted
     * transaction is still addressable.
     */
    @Operation(summary = "Revert a transaction to a history snapshot")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "string")))
    @PostMapping("/history/{historyId}/revert")
    public ResponseEntity<?> revert(@PathVariable @Positive Long historyId) {
        String currentUserId = userService.loggedInUser().getId();
        transactionHistoryService.revert(currentUserId, historyId);
        return Response.ok("Transaction reverted successfully");
    }
}
