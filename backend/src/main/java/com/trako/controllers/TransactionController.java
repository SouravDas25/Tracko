package com.trako.controllers;

import com.trako.dtos.TransactionDetailDTO;
import com.trako.dtos.TransactionPeriodSummaryDTO;
import com.trako.dtos.TransactionSummaryDTO;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
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
                        && t.getTransactionType() != null && t.getTransactionType() == 2)) // hide CREDIT side
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
                t.setTransactionType(3); // mark as TRANSFER for response rendering
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
                            && dto.getTransactionType() != null && dto.getTransactionType() == 2);
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
                dto.setTransactionType(3); // mark as TRANSFER for response rendering
            }
        }
        return dtos;
    }

    /**
     * GET /api/transactions
     * Returns paginated transactions for the authenticated user for a specific month/year.
     * Transfer credit-side entries are hidden, and transfer transactions are labeled as type=TRANSFER in response.
     */
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
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
            
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
                
                Map<String, Object> payload = new HashMap<>();
                payload.put("month", month);
                payload.put("year", resolvedYear);
                payload.put("page", dtoPage.getNumber());
                payload.put("size", dtoPage.getSize());
                payload.put("totalElements", dtoPage.getTotalElements());
                payload.put("totalPages", dtoPage.getTotalPages());
                payload.put("hasNext", dtoPage.hasNext());
                payload.put("hasPrevious", dtoPage.hasPrevious());
                payload.put("transactions", dtos);
                
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

            Map<String, Object> payload = new HashMap<>();
            payload.put("month", month);
            payload.put("year", resolvedYear);
            payload.put("page", transactionPage.getNumber());
            payload.put("size", transactionPage.getSize());
            payload.put("totalElements", transactionPage.getTotalElements());
            payload.put("totalPages", transactionPage.getTotalPages());
            payload.put("hasNext", transactionPage.hasNext());
            payload.put("hasPrevious", transactionPage.hasPrevious());
            payload.put("transactions", transactions);

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
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TransactionRequest request) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            
            // Check if this is a transfer request (has toAccountId field)
            if (request.isTransfer()) {
                // This is a TRANSFER request
                log.info("Processing transfer request from account {} to account {}", 
                        request.getSourceAccountId(), request.toAccountId());
                
                // Validate required fields
                Long fromAccountId = request.getSourceAccountId();
                if (fromAccountId == null) {
                    return Response.badRequest("Transfer requires fromAccountId or accountId");
                }
                if (request.toAccountId() == null) {
                    return Response.badRequest("Transfer requires toAccountId");
                }
                if (request.amount() == null || request.amount() <= 0) {
                    return Response.badRequest("Transfer amount must be greater than 0");
                }
                
                // Delegate to service layer for transfer creation
                Transaction[] result = transactionWriteService.createTransfer(
                    currentUserId,
                    fromAccountId,
                    request.toAccountId(),
                    request.date(),
                    request.amount(),
                    request.name(),
                    request.comments()
                );
                
                // Return the debit side (represents the transfer in UI)
                return Response.ok(result[0], "Transfer created successfully");
                
            } else {
                // This is a REGULAR TRANSACTION request
                Transaction saved = transactionWriteService.createTransaction(currentUserId, request);
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
     * <p>Delegates all write logic to TransactionWriteService.
     * <p>If the transaction is part of a transfer (has linkedTransactionId), both sides are updated atomically.
     * <p>For regular transactions, verifies that existing/new account plus category are all owned by authenticated user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable @Positive Long id, @Valid @RequestBody TransactionRequest request) {
        try {
            String currentUserId = userService.loggedInUser().getId();

            // Load an existing transaction to check its type and support partial updates
            Transaction existing = transactionService.findById(id).orElse(null);
            if (existing == null) {
                return Response.notFound("Transaction not found");
            }

            // Check if an existing transaction is part of a transfer
            boolean isExistingTransfer = transactionWriteService.isTransfer(id);
            
            // CASE 1: Updating a TRANSFER (either existing is a transfer, or request is converting to transfer)
            // Note: Currently we assume if existing is a transfer, we stay transfer.
            // If a request has transfer fields, we treat it as a transfer update.
            if (isExistingTransfer || request.isTransfer()) {
                // For transfer updates, `accountId` in the request represents the account of the
                // transaction being updated (debit or credit side). Map it to the correct
                // transfer side so updates are symmetric when calling PUT on either transaction.
                boolean isDebitSide = existing.getTransactionType() != null && existing.getTransactionType() == 1;
                boolean isCreditSide = existing.getTransactionType() != null && existing.getTransactionType() == 2;

                // Important: request.getSourceAccountId() falls back to `accountId` when `fromAccountId` is null.
                // For credit-side updates, the request will usually only send `accountId` (the credit account),
                // so treating that as the *source* would invert the transfer and cause validation failures.
                Long resolvedFromAccountId = request.fromAccountId();
                Long resolvedToAccountId = request.toAccountId();

                if (resolvedFromAccountId == null && isDebitSide) {
                    resolvedFromAccountId = request.accountId();
                }
                if (resolvedToAccountId == null && isCreditSide) {
                    resolvedToAccountId = request.accountId();
                }

                Transaction[] result = transactionWriteService.updateTransfer(
                    currentUserId,
                    id,
                    resolvedFromAccountId, // null means don't change
                    resolvedToAccountId,
                    request.date(),
                    request.amount(),
                    request.name(),
                    request.comments()
                );

                Transaction updated = result[0].getId().equals(id) ? result[0] : result[1];
                return Response.ok(updated, "Transfer updated successfully");
            }

            // CASE 2: Updating a REGULAR TRANSACTION
            // Merge non-null fields from the request into a new Transaction object for the service
            //  (updateTransaction) expects a Transaction entity.
            // We will populate it with existing values first, then override with request values.
            
            Transaction txToUpdate = new Transaction();
            txToUpdate.setId(id);
            
            // Apply updates or keep existing
            txToUpdate.setAccountId(request.accountId() != null ? request.accountId() : existing.getAccountId());
            txToUpdate.setCategoryId(request.categoryId() != null ? request.categoryId() : existing.getCategoryId());
            txToUpdate.setTransactionType(request.transactionType() != null ? request.transactionType() : existing.getTransactionType());
            txToUpdate.setAmount(request.amount() != null ? request.amount() : existing.getAmount());
            txToUpdate.setName(request.name() != null ? request.name() : existing.getName());
            txToUpdate.setComments(request.comments() != null ? request.comments() : existing.getComments());
            txToUpdate.setDate(request.date() != null ? request.date() : existing.getDate());
            txToUpdate.setIsCountable(request.isCountable() != null ? request.isCountable() : existing.getIsCountable());
            
            // Currency fields - if any are provided, we should probably take them.
            // If not provided, keep existing.
            txToUpdate.setOriginalCurrency(request.originalCurrency() != null ? request.originalCurrency() : existing.getOriginalCurrency());
            txToUpdate.setOriginalAmount(request.originalAmount() != null ? request.originalAmount() : existing.getOriginalAmount());
            txToUpdate.setExchangeRate(request.exchangeRate() != null ? request.exchangeRate() : existing.getExchangeRate());
            
            // linkedTransactionId should not be set manually for regular transactions usually, 
            // but we preserve it if it was there (though if it was there, isExistingTransfer would be true)
            txToUpdate.setLinkedTransactionId(existing.getLinkedTransactionId());

            // Delegate to write service - it will enforce ownership and will also handle
            // transfer updates when the existing transaction is part of a transfer.
            Transaction updated = transactionWriteService.updateTransaction(currentUserId, txToUpdate);
            String message = "Transaction updated successfully";
            return Response.ok(updated, message);
            
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
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable @Positive Long id) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            Transaction existing = transactionService.findById(id).orElse(null);
            if (existing == null) {
                return Response.notFound("Transaction not found");
            }
            Account acc = accountRepository.findById(existing.getAccountId()).orElse(null);
            if (acc == null || !currentUserId.equals(acc.getUserId())) {
                return Response.unauthorized();
            }
            
            // Check if this is part of a transfer - delegate to service
            if (transactionWriteService.isTransfer(id)) {
                transactionWriteService.deleteTransfer(currentUserId, id);
                return Response.ok("Transfer deleted successfully");
            }
            
            // Regular transaction (not a transfer)
            transactionWriteService.deleteForUser(currentUserId, id);
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
