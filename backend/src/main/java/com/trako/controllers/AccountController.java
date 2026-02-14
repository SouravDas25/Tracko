package com.trako.controllers;

import com.trako.entities.Account;
import com.trako.models.request.AccountSaveRequest;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.services.AccountService;
import com.trako.services.UserService;
import com.trako.util.Response;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        String currentUserId = userService.loggedInUser().getId();
        List<Account> accounts = accountService.findByUserId(currentUserId);
        return Response.ok(accounts);
    }

    @GetMapping("/balances")
    public ResponseEntity<?> getMyAccountBalances() {
        String currentUserId = userService.loggedInUser().getId();

        Long transferCategoryId = null;
        var transferCats = categoryRepository.findByUserIdAndName(currentUserId, "TRANSFER");
        if (transferCats != null && !transferCats.isEmpty()) {
            transferCategoryId = transferCats.get(0).getId();
        }

        final var rows = transactionRepository.findAccountBalancesByUserId(currentUserId, transferCategoryId);
        Map<Long, Double> balances = new HashMap<>();
        for (var row : rows) {
            Object accountIdObj = row.get("accountId");
            Object balanceObj = row.get("balance");
            if (!(accountIdObj instanceof Number) || !(balanceObj instanceof Number)) {
                continue;
            }
            Long accountId = ((Number) accountIdObj).longValue();
            Double balance = ((Number) balanceObj).doubleValue();
            balances.put(accountId, balance);
        }
        return Response.ok(balances);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        String currentUserId = userService.loggedInUser().getId();
        Account account = accountService.findById(id).orElse(null);
        if (account == null) {
            return notFound("Account not found");
        }
        if (!currentUserId.equals(account.getUserId())) {
            return Response.unauthorized();
        }

        Long transferCategoryId = null;
        var transferCats = categoryRepository.findByUserIdAndName(currentUserId, "TRANSFER");
        if (transferCats != null && !transferCats.isEmpty()) {
            transferCategoryId = transferCats.get(0).getId();
        }
        Double balance = transactionRepository.findBalanceByAccountId(id, transferCategoryId);
        account.setBalance(balance == null ? 0.0 : balance);

        return Response.ok(account);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUserId(@PathVariable String userId) {
        String currentUserId = userService.loggedInUser().getId();
        if (!currentUserId.equals(userId)) {
            return Response.unauthorized();
        }
        List<Account> accounts = accountService.findByUserId(currentUserId);
        return Response.ok(accounts);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AccountSaveRequest request) {
        Account account = new Account();
        account.setName(request.getName());
        if (request.getCurrency() != null) {
            account.setCurrency(request.getCurrency());
        }
        String currentUserId = userService.loggedInUser().getId();
        account.setUserId(currentUserId);
        Account saved = accountService.save(account);
        return Response.ok(saved, "Account created successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody AccountSaveRequest request) {
        String currentUserId = userService.loggedInUser().getId();
        Account existing = accountService.findById(id).orElse(null);
        if (existing == null) {
            return notFound("Account not found");
        }
        if (!currentUserId.equals(existing.getUserId())) {
            return Response.unauthorized();
        }

        existing.setName(request.getName());
        if (request.getCurrency() != null) {
            existing.setCurrency(request.getCurrency());
        }

        Account updated = accountService.save(existing);
        return Response.ok(updated, "Account updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        String currentUserId = userService.loggedInUser().getId();
        Account existing = accountService.findById(id).orElse(null);
        if (existing == null) {
            return notFound("Account not found");
        }
        if (!currentUserId.equals(existing.getUserId())) {
            return Response.unauthorized();
        }
        accountService.delete(id);
        return Response.ok("Account deleted successfully");
    }
}
