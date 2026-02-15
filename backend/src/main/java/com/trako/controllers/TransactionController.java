package com.trako.controllers;

import com.trako.dtos.TransactionDetailDTO;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
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

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
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

    private static final ObjectMapper ACCOUNT_IDS_OBJECT_MAPPER = new ObjectMapper();

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
            @RequestParam Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "500") Integer size) {
        try {
            if (month == null || month < 1 || month > 12) {
                return Response.badRequest("month must be between 1 and 12");
            }
            if (page == null || page < 0) {
                return Response.badRequest("page must be 0 or greater");
            }
            if (size == null || size < 1 || size > 500) {
                return Response.badRequest("size must be between 1 and 500");
            }

            int resolvedYear = (year == null) ? Calendar.getInstance().get(Calendar.YEAR) : year;
            Date startDate = getStartOfMonth(resolvedYear, month);
            Date endDate = getStartOfNextMonth(resolvedYear, month);

            String currentUserId = userService.loggedInUser().getId();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
            Page<Transaction> transactionPage = transactionService.findByUserIdAndDateBetween(
                    currentUserId,
                    startDate,
                    endDate,
                    pageable
            );

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
     * GET /api/transactions/date-range
     * Returns authenticated user's transactions between startDate and endDate.
     * If accountIds is provided (comma-separated), results are filtered by those accounts.
     * Transfer credit-side entries are hidden, and transfer transactions are labeled as type=TRANSFER.
     * If expand=true, returns full details (Account, Category, Splits) to avoid N+1 queries.
     */
    @GetMapping("/date-range")
    public ResponseEntity<?> getMyTransactionsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) String accountIds,
            @RequestParam(defaultValue = "false") boolean expand) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            List<Long> ids = com.trako.util.CommonUtil.parseAccountIds(accountIds);
            
            if (expand) {
                List<TransactionDetailDTO> dtos = transactionService.findWithDetailsByUserIdAndDateBetween(currentUserId, startDate, endDate, ids);
                dtos = hideTransferCreditsForDTO(dtos, currentUserId);
                dtos = markTransferTypeAsTransferForDTO(dtos, currentUserId);
                return Response.ok(dtos);
            }

            List<Transaction> transactions;
            if (ids.isEmpty()) {
                transactions = transactionService.findByUserIdAndDateBetween(currentUserId, startDate, endDate);
            } else {
                transactions = transactionService.findByUserIdAndDateBetweenAndAccountIds(currentUserId, startDate, endDate, ids);
            }
            transactions = hideTransferCredits(transactions, currentUserId);
            transactions = markTransferTypeAsTransfer(transactions, currentUserId);
            return Response.ok(transactions);
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
    public ResponseEntity<?> getById(@PathVariable Long id) {
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
     * GET /api/transactions/user/{userId}
     * Returns all transactions for the specified user, but only when userId matches the authenticated user.
     * Transfer credit-side entries are hidden, and transfer transactions are labeled as type=TRANSFER.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUserId(@PathVariable String userId) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            if (!currentUserId.equals(userId)) {
                return Response.unauthorized();
            }
            List<Transaction> transactions = transactionService.findByUserId(currentUserId);
            transactions = hideTransferCredits(transactions, currentUserId);
            transactions = markTransferTypeAsTransfer(transactions, currentUserId);
            return Response.ok(transactions);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    /**
     * GET /api/transactions/user/{userId}/date-range
     * Returns transactions in the date range for the specified user, only when userId matches the authenticated user.
     * Transfer credit-side entries are hidden, and transfer transactions are labeled as type=TRANSFER.
     */
    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<?> getByUserIdAndDateRange(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            if (!currentUserId.equals(userId)) {
                return Response.unauthorized();
            }
            List<Transaction> transactions = transactionService.findByUserIdAndDateBetween(currentUserId, startDate, endDate);
            transactions = hideTransferCredits(transactions, currentUserId);
            transactions = markTransferTypeAsTransfer(transactions, currentUserId);
            return Response.ok(transactions);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    /**
     * GET /api/transactions/account/{accountId}
     * Returns all transactions for the given account id.
     * When a user is authenticated, transfer entries are normalized for UI (hide credit side, mark transfer type).
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getByAccountId(@PathVariable Long accountId) {
        List<Transaction> transactions = transactionService.findByAccountId(accountId);
        try {
            String currentUserId = userService.loggedInUser().getId();
            transactions = hideTransferCredits(transactions, currentUserId);
            transactions = markTransferTypeAsTransfer(transactions, currentUserId);
        } catch (UserNotLoggedInException e) {
            // If unauthenticated, return raw list (consistent with previous behavior)
        }
        return Response.ok(transactions);
    }

    /**
     * GET /api/transactions/category/{categoryId}
     * Returns all transactions for the given category id.
     * When a user is authenticated, transfer entries are normalized for UI (hide credit side, mark transfer type).
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getByCategoryId(@PathVariable Long categoryId) {
        List<Transaction> transactions = transactionService.findByCategoryId(categoryId);
        try {
            String currentUserId = userService.loggedInUser().getId();
            transactions = hideTransferCredits(transactions, currentUserId);
            transactions = markTransferTypeAsTransfer(transactions, currentUserId);
        } catch (UserNotLoggedInException e) {
            // If unauthenticated, return raw list
        }
        return Response.ok(transactions);
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
     * 
     * <p>Delegates all write logic to TransactionWriteService.
     * <p>If the transaction is part of a transfer (has linkedTransactionId), both sides are updated atomically.
     * <p>For regular transactions, verifies that existing/new account plus category are all owned by authenticated user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        try {
            String currentUserId = userService.loggedInUser().getId();

            // Map request to entity for the write service
            Transaction tx = new Transaction();
            tx.setId(id);
            tx.setAccountId(request.accountId());
            tx.setCategoryId(request.categoryId());
            tx.setTransactionType(request.transactionType());
            tx.setAmount(request.amount());
            tx.setName(request.name());
            tx.setComments(request.comments());
            tx.setDate(request.date());
            tx.setIsCountable(request.isCountable());
            tx.setOriginalCurrency(request.originalCurrency());
            tx.setOriginalAmount(request.originalAmount());
            tx.setExchangeRate(request.exchangeRate());
            tx.setLinkedTransactionId(request.linkedTransactionId());

            // Delegate to write service - it handles both regular transactions and transfers
            Transaction updated = transactionWriteService.updateTransaction(currentUserId, tx);
            
            // Determine the success message based on whether it's a transfer
            String message = transactionWriteService.isTransfer(id) ? 
                "Transfer updated successfully" : "Transaction updated successfully";
            
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
    public ResponseEntity<?> delete(@PathVariable Long id) {
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
     * GET /api/transactions/user/{userId}/summary
     * Returns summary for the specified user and date range, only when userId matches authenticated user.
     */
    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<?> getSummary(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false, defaultValue = "true") boolean includeRollover) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            if (!currentUserId.equals(userId)) {
                return Response.unauthorized();
            }
            TransactionSummaryDTO summary;
            if (includeRollover) {
                summary = transactionService.getSummaryWithRollover(currentUserId, startDate, endDate, null);
            } else {
                summary = transactionService.getSummary(currentUserId, startDate, endDate);
            }
            return Response.ok(summary);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    /**
     * GET /api/transactions/user/{userId}/total-income
     * Returns total income for the specified user in the date range, only when userId matches authenticated user.
     */
    @GetMapping("/user/{userId}/total-income")
    public ResponseEntity<?> getTotalIncome(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            if (!currentUserId.equals(userId)) {
                return Response.unauthorized();
            }
            Double totalIncome = transactionService.getTotalIncome(currentUserId, startDate, endDate);
            return Response.ok(totalIncome);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    /**
     * GET /api/transactions/user/{userId}/total-expense
     * Returns total expense for the specified user in the date range, only when userId matches authenticated user.
     */
    @GetMapping("/user/{userId}/total-expense")
    public ResponseEntity<?> getTotalExpense(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            if (!currentUserId.equals(userId)) {
                return Response.unauthorized();
            }
            Double totalExpense = transactionService.getTotalExpense(currentUserId, startDate, endDate);
            return Response.ok(totalExpense);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }
}
