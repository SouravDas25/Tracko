package com.trako.controllers;

import com.trako.dtos.SearchRequestDTO;
import com.trako.dtos.TransactionDetailDTO;
import com.trako.dtos.TransactionPeriodSummaryDTO;
import com.trako.dtos.TransactionSearchResultDTO;
import com.trako.dtos.TransactionSummaryDTO;
import com.trako.dtos.TransactionsPageDTO;
import com.trako.entities.Account;
import com.trako.entities.Transaction;
import com.trako.enums.TransactionDbType;
import com.trako.entities.User;
import com.trako.models.request.TransactionRequest;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.services.TransactionSearchService;
import com.trako.services.UserService;
import com.trako.services.transactions.TransactionService;
import com.trako.services.transactions.TransactionWriteService;
import com.trako.util.CommonUtil;
import com.trako.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Tag(name = "Transactions", description = "Create, read, update and delete transactions and transfers")
@RestController
@RequestMapping("/api/transactions")
@Validated
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionWriteService transactionWriteService;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionSearchService transactionSearchService;

    /**
     * Applies transfer rendering rules to a page of transactions in a single pass:
     * hides the CREDIT side of each transfer and labels the remaining (DEBIT) side as TRANSFER.
     * Resolves the user's TRANSFER category once to avoid redundant lookups.
     */
    private List<Transaction> applyTransferRendering(List<Transaction> transactions, String userId) {
        var transferCats = categoryRepository.findByUserIdAndName(userId, "TRANSFER");
        if (transferCats == null || transferCats.isEmpty()) {
            return transactions;
        }
        Long transferCategoryId = transferCats.get(0).getId();
        List<Transaction> result = new ArrayList<>(transactions.size());
        for (var t : transactions) {
            boolean isTransfer = t.getCategoryId() != null
                    && t.getCategoryId().equals(transferCategoryId)
                    && t.getIsCountable() != null && t.getIsCountable() == 0;
            if (isTransfer && t.getTransactionType() != null && t.getTransactionType() == TransactionDbType.CREDIT) {
                continue; // hide CREDIT side of transfer
            }
            if (isTransfer) {
                t.setRenderedTransactionType(TransactionDbType.TRANSFER_RENDERING_VALUE); // mark DEBIT side as TRANSFER for rendering
            }
            result.add(t);
        }
        return result;
    }

    /**
     * DTO counterpart of {@link #applyTransferRendering(List, String)}: hides the CREDIT side of
     * each transfer and labels the remaining (DEBIT) side as TRANSFER, resolving the TRANSFER
     * category once.
     */
    private List<TransactionDetailDTO> applyTransferRenderingForDTO(List<TransactionDetailDTO> dtos, String userId) {
        var transferCats = categoryRepository.findByUserIdAndName(userId, "TRANSFER");
        if (transferCats == null || transferCats.isEmpty()) {
            return dtos;
        }
        Long transferCategoryId = transferCats.get(0).getId();
        List<TransactionDetailDTO> result = new ArrayList<>(dtos.size());
        for (var dto : dtos) {
            boolean isTransfer = dto.getCategoryId() != null
                    && dto.getCategoryId().equals(transferCategoryId)
                    && dto.getIsCountable() != null && dto.getIsCountable() == 0;
            if (isTransfer && dto.getTransactionType() != null && dto.getTransactionType() == TransactionDbType.CREDIT.getValue()) {
                continue; // hide CREDIT side of transfer
            }
            if (isTransfer) {
                dto.setTransactionType(TransactionDbType.TRANSFER_RENDERING_VALUE); // mark DEBIT side as TRANSFER for rendering
            }
            result.add(dto);
        }
        return result;
    }

    /**
     * Returns true when both bounds are present and the end falls before the start.
     * A null bound is treated as an open range and passes validation, since some
     * endpoints allow an omitted start or end.
     */
    private static boolean isEndBeforeStart(Date startDate, Date endDate) {
        return startDate != null && endDate != null && endDate.before(startDate);
    }

    /**
     * GET /api/transactions/search
     * Searches transactions across all time periods using free-text query with fuzzy matching.
     * Supports optional filters for date range, amount range, accounts, and category.
     */
    @Operation(summary = "Search transactions across all time periods")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TransactionSearchResultDTO.class)))
    @GetMapping("/search")
    public ResponseEntity<?> searchTransactions(
            @RequestParam @NotBlank @Size(max = 200, message = "Search query must not exceed 200 characters") String query,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String accountIds,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0.7") Double fuzzyThreshold,
            @RequestParam(defaultValue = "false") Boolean expand) {
        // Validate query
        if (query == null || query.isBlank()) {
            return Response.badRequest("Search query cannot be empty");
        }

        // Validate size
        if (size < 1 || size > 100) {
            return Response.badRequest("Page size must be between 1 and 100");
        }

        // Validate date range
        if (isEndBeforeStart(startDate, endDate)) {
            return Response.badRequest("End date must be after start date");
        }

        String currentUserId = userService.loggedInUser().getId();

        // Build SearchRequestDTO from query params
        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery(query);
        request.setPage(page);
        request.setSize(size);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setMinAmount(minAmount);
        request.setMaxAmount(maxAmount);
        request.setAccountIds(CommonUtil.parseAccountIds(accountIds));
        request.setCategoryId(categoryId);
        request.setFuzzyThreshold(fuzzyThreshold);
        request.setExpand(expand);

        TransactionSearchResultDTO result = transactionSearchService.search(currentUserId, request);
        return Response.ok(result);
    }

    /**
     * GET /api/transactions
     * Returns paginated transactions for the authenticated user for a specific month/year.
     * Transfer credit-side entries are hidden, and transfer transactions are labeled as type=TRANSFER in response.
     */
    @Operation(summary = "List transactions with optional filters")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TransactionsPageDTO.class)))
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) String accountIds,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "500") Integer size,
            @RequestParam(defaultValue = "false") boolean expand) {
        if (page == null || page < 0) {
            return Response.badRequest("page must be 0 or greater");
        }
        if (size == null || size < 1 || size > 10000) {
            return Response.badRequest("size must be between 1 and 10000");
        }

        Date start, end;
        int resolvedYear = (year == null) ? Calendar.getInstance().get(Calendar.YEAR) : year;

        if (startDate != null && endDate != null) {
            if (isEndBeforeStart(startDate, endDate)) {
                return Response.badRequest("End date must be after start date");
            }
            start = startDate;
            end = endDate;
        } else if (month != null) {
            if (month < 1 || month > 12) {
                return Response.badRequest("month must be between 1 and 12");
            }
            start = getStartOfMonth(resolvedYear, month);
            end = getStartOfNextMonth(resolvedYear, month);
        } else {
            return Response.badRequest("Either month or startDate/endDate must be provided");
        }

        String currentUserId = userService.loggedInUser().getId();
        List<Long> ids = CommonUtil.parseAccountIds(accountIds);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id")));

        if (expand) {
            Page<TransactionDetailDTO> dtoPage;
            if (categoryId != null && categoryId > 0) {
                if (ids.isEmpty()) {
                    dtoPage = transactionService.findWithDetailsByUserIdAndCategoryIdAndDateBetween(
                            currentUserId,
                            categoryId,
                            start,
                            end,
                            pageable
                    );
                } else {
                    dtoPage = transactionService.findWithDetailsByUserIdAndCategoryIdAndDateBetweenAndAccountIds(
                            currentUserId,
                            categoryId,
                            start,
                            end,
                            ids,
                            pageable
                    );
                }
            } else {
                dtoPage = transactionService.findWithDetailsByUserIdAndDateBetween(
                        currentUserId,
                        start,
                        end,
                        ids,
                        pageable
                );
            }

            List<TransactionDetailDTO> dtos = applyTransferRenderingForDTO(new ArrayList<>(dtoPage.getContent()), currentUserId);

            TransactionsPageDTO payload = new TransactionsPageDTO();
            payload.setMonth(month);
            payload.setYear(resolvedYear);
            payload.setPage(dtoPage.getNumber());
            payload.setSize(dtoPage.getSize());
            payload.setTotalElements(dtoPage.getTotalElements());
            payload.setTotalPages(dtoPage.getTotalPages());
            payload.setHasNext(dtoPage.hasNext());
            payload.setHasPrevious(dtoPage.hasPrevious());
            payload.setTransactions(dtos);

            return Response.ok(payload);
        }

        Page<Transaction> transactionPage;
        if (categoryId != null && categoryId > 0) {
            if (ids.isEmpty()) {
                transactionPage = transactionService.findByUserIdAndCategoryIdAndDateBetween(
                        currentUserId,
                        categoryId,
                        start,
                        end,
                        pageable
                );
            } else {
                transactionPage = transactionService.findByUserIdAndCategoryIdAndDateBetweenAndAccountIds(
                        currentUserId,
                        categoryId,
                        start,
                        end,
                        ids,
                        pageable
                );
            }
        } else if (ids.isEmpty()) {
            transactionPage = transactionService.findByUserIdAndDateBetween(
                    currentUserId,
                    start,
                    end,
                    pageable
            );
        } else {
            transactionPage = transactionService.findByUserIdAndDateBetweenAndAccountIds(
                    currentUserId,
                    start,
                    end,
                    ids,
                    pageable
            );
        }

        List<Transaction> transactions = applyTransferRendering(new ArrayList<>(transactionPage.getContent()), currentUserId);

        TransactionsPageDTO payload = new TransactionsPageDTO();
        payload.setMonth(month);
        payload.setYear(resolvedYear);
        payload.setPage(transactionPage.getNumber());
        payload.setSize(transactionPage.getSize());
        payload.setTotalElements(transactionPage.getTotalElements());
        payload.setTotalPages(transactionPage.getTotalPages());
        payload.setHasNext(transactionPage.hasNext());
        payload.setHasPrevious(transactionPage.hasPrevious());
        payload.setTransactions(transactions);

        return Response.ok(payload);
    }

    private Date getStartOfMonth(int year, int month) {
        return startOfMonth(year, month, 0);
    }

    private Date getStartOfNextMonth(int year, int month) {
        return startOfMonth(year, month, 1);
    }

    private Date startOfMonth(int year, int month, int monthOffset) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month - 1, 1, 0, 0, 0);
        calendar.add(Calendar.MONTH, monthOffset);
        return calendar.getTime();
    }

    /**
     * GET /api/transactions/total-income
     * Returns total income for the currently authenticated user within the provided date range (inclusive).
     */
    @Operation(summary = "Get total income in a date range")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "number", format = "double")))
    @GetMapping("/total-income")
    public ResponseEntity<?> getMyTotalIncome(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        if (isEndBeforeStart(startDate, endDate)) {
            return Response.badRequest("End date must be after start date");
        }
        String currentUserId = userService.loggedInUser().getId();
        Double totalIncome = transactionService.getTotalIncome(currentUserId, startDate, endDate);
        return Response.ok(totalIncome);
    }

    /**
     * GET /api/transactions/total-expense
     * Returns total expense for the currently authenticated user within the provided date range (inclusive).
     */
    @Operation(summary = "Get total expense in a date range")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "number", format = "double")))
    @GetMapping("/total-expense")
    public ResponseEntity<?> getMyTotalExpense(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        if (isEndBeforeStart(startDate, endDate)) {
            return Response.badRequest("End date must be after start date");
        }
        String currentUserId = userService.loggedInUser().getId();
        Double totalExpense = transactionService.getTotalExpense(currentUserId, startDate, endDate);
        return Response.ok(totalExpense);
    }

    /**
     * GET /api/transactions/summary
     * Returns income/expense/balance summary for the authenticated user in the date range.
     * If accountIds are provided (comma-separated), the summary is limited to those accounts.
     */
    @Operation(summary = "Get income/expense summary in a date range")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TransactionSummaryDTO.class)))
    @GetMapping("/summary")
    public ResponseEntity<?> getMySummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) String accountIds,
            @RequestParam(required = false, defaultValue = "true") boolean includeRollover,
            @RequestParam(required = false) Long categoryId) {
        if (isEndBeforeStart(startDate, endDate)) {
            return Response.badRequest("End date must be after start date");
        }
        String currentUserId = userService.loggedInUser().getId();
        List<Long> ids = CommonUtil.parseAccountIds(accountIds);
        TransactionSummaryDTO summary;
        if (includeRollover) {
            summary = transactionService.getSummaryWithRollover(currentUserId, startDate, endDate, ids, categoryId);
        } else {
            summary = transactionService.getSummary(currentUserId, startDate, endDate, ids, categoryId);
        }
        return Response.ok(summary);
    }

    /**
     * GET /api/transactions/{id}
     * Returns a single transaction by id only if it belongs to the authenticated user
     * (ownership verified through the transaction's account).
     */
    @Operation(summary = "Get a transaction by ID")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Transaction.class)))
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable @Positive Long id) {
        String currentUserId = userService.loggedInUser().getId();
        Transaction tx = transactionService.findById(id).orElse(null);
        if (tx == null) {
            return Response.notFound("Transaction not found");
        }
        Account acc = accountRepository.findById(tx.getAccountId()).orElse(null);
        if (acc == null || !currentUserId.equals(acc.getUserId())) {
            return Response.unauthorized();
        }
        return Response.ok(tx);
    }

    /**
     * POST /api/transactions
     * Creates a new transaction OR transfer for the authenticated user.
     *
     * <p>For regular transactions: Include accountId, categoryId, transactionType, amount, etc.
     * <p>For transfers: Include accountId (or fromAccountId), toAccountId, and amount.
     * The presence of toAccountId indicates this is a transfer request.
     *
     * <p>Validates that accounts and categories exist and are owned by the current user before saving.
     */
    @Operation(summary = "Create a transaction or transfer")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Transaction.class)))
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TransactionRequest request) {
        User user = userService.loggedInUser();
        String currentUserId = user.getId();

        Transaction saved = transactionWriteService.createUnifiedTransaction(currentUserId, request);

        if (request.isTransfer()) {
            return Response.ok(saved, "Transfer created successfully");
        } else {
            return Response.ok(saved, "Transaction created successfully");
        }
    }

    /**
     * PUT /api/transactions/{id}
     * Updates an existing transaction or transfer by id.
     * Supports partial updates - only non-null fields in the request are updated.
     *
     * <p>Delegates all writing logic to TransactionWriteService.
     */
    @Operation(summary = "Update a transaction or transfer")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Transaction.class)))
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable @Positive Long id, @Valid @RequestBody TransactionRequest request) {
        User user = userService.loggedInUser();
        String currentUserId = user.getId();

        Transaction updated = transactionWriteService.updateTransaction(currentUserId, id, request);
        return Response.ok(updated, "Transaction updated successfully");
    }

    /**
     * DELETE /api/transactions/{id}
     * Deletes a transaction only if it exists and belongs to the authenticated user.
     * If the transaction is part of a transfer (has linkedTransactionId), both sides are deleted atomically.
     */
    @Operation(summary = "Delete a transaction or transfer")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "string")))
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable @Positive Long id) {
        String currentUserId = userService.loggedInUser().getId();

        transactionWriteService.deleteUnifiedTransaction(currentUserId, id);

        return Response.ok("Transaction deleted successfully");
    }

    /**
     * GET /api/transactions/summary/monthly
     * Returns a list of summaries grouped by month for a specific year.
     */
    @Operation(summary = "Monthly summaries for a year")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionPeriodSummaryDTO.class))))
    @GetMapping("/summary/monthly")
    public ResponseEntity<?> getMonthlySummaries(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String accountIds,
            @RequestParam(required = false) Long categoryId) {
        String currentUserId = userService.loggedInUser().getId();
        List<Long> ids = CommonUtil.parseAccountIds(accountIds);

        int resolvedYear = (year == null) ? Calendar.getInstance().get(Calendar.YEAR) : year;

        List<TransactionPeriodSummaryDTO> summaries = transactionService.getMonthlySummaries(currentUserId, resolvedYear, ids, categoryId);
        return Response.ok(summaries);
    }

    /**
     * GET /api/transactions/summary/yearly
     * Returns a list of summaries grouped by year.
     */
    @Operation(summary = "Yearly summaries")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionPeriodSummaryDTO.class))))
    @GetMapping("/summary/yearly")
    public ResponseEntity<?> getYearlySummaries(
            @RequestParam(required = false) String accountIds,
            @RequestParam(required = false) Long categoryId) {
        String currentUserId = userService.loggedInUser().getId();
        List<Long> ids = CommonUtil.parseAccountIds(accountIds);

        List<TransactionPeriodSummaryDTO> summaries = transactionService.getYearlySummaries(currentUserId, ids, categoryId);
        return Response.ok(summaries);
    }

}
