package com.trako.controllers;

import com.trako.dtos.TransactionDetailDTO;
import com.trako.dtos.TransactionPeriodSummaryDTO;
import com.trako.dtos.TransactionSummaryDTO;
import com.trako.entities.Account;
import com.trako.entities.Transaction;
import com.trako.entities.TransactionType;
import com.trako.entities.User;
import com.trako.exceptions.AuthorizationException;
import com.trako.exceptions.NotFoundException;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.models.request.TransactionRequest;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.services.TransactionService;
import com.trako.services.TransactionWriteService;
import com.trako.services.UserService;
import com.trako.util.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

// OpenAPI annotations
import io.swagger.v3.oas.annotations.Operation;
import com.trako.dtos.TransactionsPageDTO;

@RestController
@RequestMapping("/api/transactions")
@Validated
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

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

    private List<Transaction> hideTransferCredits(List<Transaction> transactions, String userId) {
        var transferCats = categoryRepository.findByUserIdAndName(userId, "TRANSFER");
        if (transferCats == null || transferCats.isEmpty()) {
            return transactions;
        }
        Long transferCategoryId = transferCats.get(0).getId();
        return transactions.stream()
                .filter(t -> !(t.getCategoryId() != null
                        && t.getCategoryId().equals(transferCategoryId)
                        && t.getIsCountable() != null && t.getIsCountable() == 0
                        && t.getTransactionType() != null && t.getTransactionType() == TransactionType.CREDIT)) // hide CREDIT side
                .collect(Collectors.toList());
    }

    private List<Transaction> markTransferTypeAsTransfer(List<Transaction> transactions, String userId) {
        var transferCats = categoryRepository.findByUserIdAndName(userId, "TRANSFER");
        if (transferCats == null || transferCats.isEmpty()) {
            return transactions;
        }
        Long transferCategoryId = transferCats.get(0).getId();
        for (var t : transactions) {
            if (t.getCategoryId() != null && t.getCategoryId().equals(transferCategoryId)
                    && t.getIsCountable() != null && t.getIsCountable() == 0) {
                t.setRenderedTransactionType(TransactionType.TRANSFER_RENDERING_VALUE); // mark as TRANSFER for response rendering
            }
        }
        return transactions;
    }

    private List<TransactionDetailDTO> hideTransferCreditsForDTO(List<TransactionDetailDTO> dtos, String userId) {
        var transferCats = categoryRepository.findByUserIdAndName(userId, "TRANSFER");
        if (transferCats == null || transferCats.isEmpty()) {
            return dtos;
        }
        Long transferCategoryId = transferCats.get(0).getId();
        return dtos.stream()
                .filter(dto -> {
                    return !(dto.getCategoryId() != null
                            && dto.getCategoryId().equals(transferCategoryId)
                            && dto.getIsCountable() != null && dto.getIsCountable() == 0
                            && dto.getTransactionType() != null && dto.getTransactionType() == TransactionType.CREDIT.getValue());
                })
                .collect(Collectors.toList());
    }

    private List<TransactionDetailDTO> markTransferTypeAsTransferForDTO(List<TransactionDetailDTO> dtos, String userId) {
        var transferCats = categoryRepository.findByUserIdAndName(userId, "TRANSFER");
        if (transferCats == null || transferCats.isEmpty()) {
            return dtos;
        }
        Long transferCategoryId = transferCats.get(0).getId();
        for (var dto : dtos) {
            if (dto.getCategoryId() != null && dto.getCategoryId().equals(transferCategoryId)
                    && dto.getIsCountable() != null && dto.getIsCountable() == 0) {
                dto.setTransactionType(TransactionType.TRANSFER_RENDERING_VALUE); // mark as TRANSFER for response rendering
            }
        }
        return dtos;
    }

    /**
     * GET /api/transactions
     * Returns paginated transactions for the authenticated user for a specific month/year.
     * Transfer credit-side entries are hidden, and transfer transactions are labeled as type=TRANSFER in response.
     */
    @Operation(summary = "List transactions with optional filters")
    @GetMapping
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "OK",
        content = @io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = @io.swagger.v3.oas.annotations.media.Schema(
                implementation = com.trako.dtos.TransactionsPageDTO.class
            )
        )
    )
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
        try {
            if (page == null || page < 0) {
                return Response.badRequest("page must be 0 or greater");
            }
            if (size == null || size < 1 || size > 10000) {
                return Response.badRequest("size must be between 1 and 10000");
            }

            Date start, end;
            int resolvedYear = (year == null) ? Calendar.getInstance().get(Calendar.YEAR) : year;

            if (startDate != null && endDate != null) {
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
            List<Long> ids = com.trako.util.CommonUtil.parseAccountIds(accountIds);
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id")));
            
            if (expand) {
                Page<TransactionDetailDTO> dtoPage;
                if (categoryId != null && categoryId > 0) {
                    dtoPage = transactionService.findWithDetailsByUserIdAndCategoryIdAndDateBetween(
                            currentUserId,
                            categoryId,
                            start,
                            end,
                            pageable
                    );
                } else {
                    dtoPage = transactionService.findWithDetailsByUserIdAndDateBetween(
                            currentUserId,
                            start,
                            end,
                            ids,
                            pageable
                    );
                }
                
                List<TransactionDetailDTO> dtos = new ArrayList<>(dtoPage.getContent());
                dtos = hideTransferCreditsForDTO(dtos, currentUserId);
                dtos = markTransferTypeAsTransferForDTO(dtos, currentUserId);
                
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
                transactionPage = transactionService.findByUserIdAndCategoryIdAndDateBetween(
                        currentUserId,
                        categoryId,
                        start,
                        end,
                        pageable
                );
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

            List<Transaction> transactions = new ArrayList<>(transactionPage.getContent());
            transactions = hideTransferCredits(transactions, currentUserId);
            transactions = markTransferTypeAsTransfer(transactions, currentUserId);

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
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    private Date getStartOfMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getStartOfNextMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MONTH, 1);
        return calendar.getTime();
    }

    /**
     * GET /api/transactions/total-income
     * Returns total income for the currently authenticated user within the provided date range (inclusive).
     */
    @Operation(summary = "Get total income in a date range")
    @GetMapping("/total-income")
    public ResponseEntity<?> getMyTotalIncome(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            Double totalIncome = transactionService.getTotalIncome(currentUserId, startDate, endDate);
            return Response.ok(totalIncome);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    /**
     * GET /api/transactions/total-expense
     * Returns total expense for the currently authenticated user within the provided date range (inclusive).
     */
    @Operation(summary = "Get total expense in a date range")
    @GetMapping("/total-expense")
    public ResponseEntity<?> getMyTotalExpense(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            Double totalExpense = transactionService.getTotalExpense(currentUserId, startDate, endDate);
            return Response.ok(totalExpense);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    /**
     * GET /api/transactions/summary
     * Returns income/expense/balance summary for the authenticated user in the date range.
     * If accountIds are provided (comma-separated), the summary is limited to those accounts.
     */
    @Operation(summary = "Get income/expense summary in a date range")
    @GetMapping("/summary")
    public ResponseEntity<?> getMySummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) String accountIds,
            @RequestParam(required = false, defaultValue = "true") boolean includeRollover) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            List<Long> ids = com.trako.util.CommonUtil.parseAccountIds(accountIds);
            TransactionSummaryDTO summary;
            if (includeRollover) {
                summary = transactionService.getSummaryWithRollover(currentUserId, startDate, endDate, ids);
            } else {
                summary = transactionService.getSummary(currentUserId, startDate, endDate, ids);
            }
            return Response.ok(summary);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    /**
     * GET /api/transactions/{id}
     * Returns a single transaction by id only if it belongs to the authenticated user
     * (ownership verified through the transaction's account).
     */
    @Operation(summary = "Get a transaction by ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable @Positive Long id) {
        try {
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
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
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
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TransactionRequest request) {
        try {
            User user = userService.loggedInUser();
            String currentUserId = user.getId();
            
            Transaction saved = transactionWriteService.createUnifiedTransaction(currentUserId, request);
            
            if (request.isTransfer()) {
                return Response.ok(saved, "Transfer created successfully");
            } else {
                return Response.ok(saved, "Transaction created successfully");
            }

        } catch (UserNotLoggedInException e) {
            log.warn("Transaction/Transfer create failed: User not logged in");
            return Response.unauthorized();
        } catch (NotFoundException e) {
            return Response.notFound(e.getMessage());
        } catch (AuthorizationException e) {
            return Response.unauthorized();
        } catch (IllegalArgumentException e) {
            log.warn("Transaction/Transfer create failed with validation error: {}", e.getMessage());
            return Response.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Transaction/Transfer create failed with exception: {}", e.getMessage(), e);
            return Response.badRequest("Failed to create transaction: " + e.getMessage());
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
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable @Positive Long id, @Valid @RequestBody TransactionRequest request) {
        try {
            User user = userService.loggedInUser();
            String currentUserId = user.getId();

            Transaction updated = transactionWriteService.updateTransaction(currentUserId, id, request);
            return Response.ok(updated, "Transaction updated successfully");
            
        } catch (UserNotLoggedInException e) {
            log.warn("Transaction update failed: User not logged in");
            return Response.unauthorized();
        } catch (NotFoundException e) {
            return Response.notFound(e.getMessage());
        } catch (AuthorizationException e) {
            return Response.unauthorized();
        } catch (IllegalArgumentException e) {
            log.warn("Transaction update failed with validation error: {}", e.getMessage());
            return Response.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Transaction update failed with exception: {}", e.getMessage(), e);
            return Response.badRequest("Failed to update transaction: " + e.getMessage());
        }
    }

    /**
     * DELETE /api/transactions/{id}
     * Deletes a transaction only if it exists and belongs to the authenticated user.
     * If the transaction is part of a transfer (has linkedTransactionId), both sides are deleted atomically.
     */
    @Operation(summary = "Delete a transaction or transfer")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable @Positive Long id) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            
            transactionWriteService.deleteUnifiedTransaction(currentUserId, id);
            
            return Response.ok("Transaction deleted successfully");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        } catch (NotFoundException e) {
            return Response.notFound(e.getMessage());
        } catch (AuthorizationException e) {
            return Response.unauthorized();
        } catch (IllegalArgumentException e) {
            log.warn("Delete failed with validation error: {}", e.getMessage());
            return Response.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting transaction {}: {}", id, e.getMessage(), e);
            return Response.badRequest("Failed to delete transaction: " + e.getMessage());
        }
    }

    /**
     * GET /api/transactions/summary/monthly
     * Returns a list of summaries grouped by month for a specific year.
     */
    @Operation(summary = "Monthly summaries for a year")
    @GetMapping("/summary/monthly")
    public ResponseEntity<?> getMonthlySummaries(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String accountIds) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            List<Long> ids = com.trako.util.CommonUtil.parseAccountIds(accountIds);
            
            // Default to current year if not provided
            int resolvedYear = (year == null) ? Calendar.getInstance().get(Calendar.YEAR) : year;
            
            List<TransactionPeriodSummaryDTO> summaries = transactionService.getMonthlySummaries(currentUserId, resolvedYear, ids);
            return Response.ok(summaries);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    /**
     * GET /api/transactions/summary/yearly
     * Returns a list of summaries grouped by year.
     */
    @Operation(summary = "Yearly summaries")
    @GetMapping("/summary/yearly")
    public ResponseEntity<?> getYearlySummaries(
            @RequestParam(required = false) String accountIds) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            List<Long> ids = com.trako.util.CommonUtil.parseAccountIds(accountIds);
            
            List<TransactionPeriodSummaryDTO> summaries = transactionService.getYearlySummaries(currentUserId, ids);
            return Response.ok(summaries);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

}
