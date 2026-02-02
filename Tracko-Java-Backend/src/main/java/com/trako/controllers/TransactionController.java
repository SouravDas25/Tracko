package com.trako.controllers;

import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.services.TransactionService;
import com.trako.dtos.TransactionSummaryDTO;
import com.trako.services.UserService;
import com.trako.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

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

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            String currentUserId = userService.loggedInUser().getId();
            List<Transaction> transactions = transactionService.findByUserId(currentUserId);
            transactions = hideTransferCredits(transactions, currentUserId);
            transactions = markTransferTypeAsTransfer(transactions, currentUserId);
            return Response.ok(transactions);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

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

    @GetMapping("/summary")
    public ResponseEntity<?> getMySummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) String accountIds) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            List<Long> ids = parseAccountIds(accountIds);
            TransactionSummaryDTO summary = transactionService.getSummary(currentUserId, startDate, endDate, ids);
            return Response.ok(summary);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<?> getMyTransactionsByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) String accountIds) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            List<Long> ids = parseAccountIds(accountIds);
            List<Transaction> transactions;
            if (ids == null || ids.isEmpty()) {
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

    private List<Long> parseAccountIds(String accountIds) {
        if (accountIds == null || accountIds.trim().isEmpty()) return new ArrayList<>();
        List<Long> out = new ArrayList<>();
        String[] parts = accountIds.split(",");
        for (String p : parts) {
            if (p == null) continue;
            String s = p.trim();
            if (s.isEmpty()) continue;
            try {
                out.add(Long.parseLong(s));
            } catch (Exception ignored) {
                // ignore invalid entries
            }
        }
        return out;
    }

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

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Transaction transaction) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            Account acc = accountRepository.findById(transaction.getAccountId()).orElse(null);
            if (acc == null || !currentUserId.equals(acc.getUserId())) {
                return Response.unauthorized();
            }
            Category cat = categoryRepository.findById(transaction.getCategoryId()).orElse(null);
            if (cat == null || !currentUserId.equals(cat.getUserId())) {
                return Response.unauthorized();
            }
            Transaction saved = transactionService.save(transaction);
            return Response.ok(saved, "Transaction created successfully");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Transaction transaction) {
        transaction.setId(id);
        try {
            String currentUserId = userService.loggedInUser().getId();
            Transaction existing = transactionService.findById(id).orElse(null);
            if (existing == null) {
                return Response.notFound("Transaction not found");
            }
            Account existingAcc = accountRepository.findById(existing.getAccountId()).orElse(null);
            if (existingAcc == null || !currentUserId.equals(existingAcc.getUserId())) {
                return Response.unauthorized();
            }
            Account acc = accountRepository.findById(transaction.getAccountId()).orElse(null);
            if (acc == null || !currentUserId.equals(acc.getUserId())) {
                return Response.unauthorized();
            }
            Category cat = categoryRepository.findById(transaction.getCategoryId()).orElse(null);
            if (cat == null || !currentUserId.equals(cat.getUserId())) {
                return Response.unauthorized();
            }
            Transaction updated = transactionService.save(transaction);
            return Response.ok(updated, "Transaction updated successfully");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

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
            transactionService.delete(id);
            return Response.ok("Transaction deleted successfully");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<?> getSummary(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            if (!currentUserId.equals(userId)) {
                return Response.unauthorized();
            }
            TransactionSummaryDTO summary = transactionService.getSummary(currentUserId, startDate, endDate);
            return Response.ok(summary);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

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
