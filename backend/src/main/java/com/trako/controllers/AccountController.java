package com.trako.controllers;

import com.trako.entities.Account;
import com.trako.entities.Transaction;
import com.trako.models.request.AccountSaveRequest;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.services.AccountService;
import com.trako.services.UserService;
import com.trako.util.Response;
import com.trako.exceptions.UserNotLoggedInException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.trako.util.Response.notFound;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<Account> accounts = accountService.findAll();
        return Response.ok(accounts);
    }

    @GetMapping("/balances")
    public ResponseEntity<?> getMyAccountBalances() {
        try {
            String currentUserId = userService.loggedInUser().getId();

            Long transferCategoryId = null;
            var transferCats = categoryRepository.findByUserIdAndName(currentUserId, "TRANSFER");
            if (transferCats != null && !transferCats.isEmpty()) {
                transferCategoryId = transferCats.get(0).getId();
            }

            List<Transaction> transactions = transactionRepository.findByUserId(currentUserId);
            Map<Long, Double> balances = new HashMap<>();

            for (Transaction t : transactions) {
                if (t.getAccountId() == null || t.getAmount() == null || t.getTransactionType() == null) {
                    continue;
                }

                boolean isTransfer = transferCategoryId != null
                        && t.getCategoryId() != null
                        && t.getCategoryId().equals(transferCategoryId);
                boolean include = (t.getIsCountable() != null && t.getIsCountable() == 1) || isTransfer;
                if (!include) continue;

                double current = balances.getOrDefault(t.getAccountId(), 0.0);
                if (t.getTransactionType() == 2) { // CREDIT
                    current += t.getAmount();
                } else if (t.getTransactionType() == 1) { // DEBIT
                    current -= t.getAmount();
                }
                balances.put(t.getAccountId(), current);
            }

            return Response.ok(balances);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return accountService.findById(id)
                .map(Response::ok)
                .orElse(notFound("Account not found"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUserId(@PathVariable String userId) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            if (!currentUserId.equals(userId)) {
                return Response.unauthorized();
            }
            List<Account> accounts = accountService.findByUserId(currentUserId);
            return Response.ok(accounts);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AccountSaveRequest request) {
        Account account = new Account();
        account.setName(request.getName());
        if (request.getCurrency() != null) {
            account.setCurrency(request.getCurrency());
        }
        try {
            String currentUserId = userService.loggedInUser().getId();
            account.setUserId(currentUserId);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
        Account saved = accountService.save(account);
        return Response.ok(saved, "Account created successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody AccountSaveRequest request) {
        Account account = new Account();
        account.setId(id);
        account.setName(request.getName());
        if (request.getCurrency() != null) {
            account.setCurrency(request.getCurrency());
        }
        try {
            String currentUserId = userService.loggedInUser().getId();
            account.setUserId(currentUserId);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
        Account updated = accountService.save(account);
        return Response.ok(updated, "Account updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        accountService.delete(id);
        return Response.ok("Account deleted successfully");
    }
}
