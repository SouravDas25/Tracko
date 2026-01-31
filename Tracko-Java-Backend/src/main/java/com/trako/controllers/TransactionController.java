package com.trako.controllers;

import com.trako.entities.Transaction;
import com.trako.services.TransactionService;
import com.trako.dtos.TransactionSummaryDTO;
import com.trako.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<Transaction> transactions = transactionService.findAll();
        return Response.ok(transactions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return transactionService.findById(id)
                .map(Response::ok)
                .orElse(Response.notFound("Transaction not found"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUserId(@PathVariable String userId) {
        List<Transaction> transactions = transactionService.findByUserId(userId);
        return Response.ok(transactions);
    }

    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<?> getByUserIdAndDateRange(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<Transaction> transactions = transactionService.findByUserIdAndDateBetween(userId, startDate, endDate);
        return Response.ok(transactions);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getByAccountId(@PathVariable Long accountId) {
        List<Transaction> transactions = transactionService.findByAccountId(accountId);
        return Response.ok(transactions);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getByCategoryId(@PathVariable Long categoryId) {
        List<Transaction> transactions = transactionService.findByCategoryId(categoryId);
        return Response.ok(transactions);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Transaction transaction) {
        Transaction saved = transactionService.save(transaction);
        return Response.ok(saved, "Transaction created successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Transaction transaction) {
        transaction.setId(id);
        Transaction updated = transactionService.save(transaction);
        return Response.ok(updated, "Transaction updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return Response.ok("Transaction deleted successfully");
    }

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<?> getSummary(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        TransactionSummaryDTO summary = transactionService.getSummary(userId, startDate, endDate);
        return Response.ok(summary);
    }

    @GetMapping("/user/{userId}/total-income")
    public ResponseEntity<?> getTotalIncome(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        Double totalIncome = transactionService.getTotalIncome(userId, startDate, endDate);
        return Response.ok(totalIncome);
    }

    @GetMapping("/user/{userId}/total-expense")
    public ResponseEntity<?> getTotalExpense(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        Double totalExpense = transactionService.getTotalExpense(userId, startDate, endDate);
        return Response.ok(totalExpense);
    }
}
