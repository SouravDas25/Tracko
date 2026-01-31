package com.trako.controllers;

import com.trako.entities.Account;
import com.trako.services.AccountService;
import com.trako.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<Account> accounts = accountService.findAll();
        return Response.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return accountService.findById(id)
                .map(Response::ok)
                .orElse(Response.notFound("Account not found"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUserId(@PathVariable String userId) {
        List<Account> accounts = accountService.findByUserId(userId);
        return Response.ok(accounts);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Account account) {
        Account saved = accountService.save(account);
        return Response.ok(saved, "Account created successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Account account) {
        account.setId(id);
        Account updated = accountService.save(account);
        return Response.ok(updated, "Account updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        accountService.delete(id);
        return Response.ok("Account deleted successfully");
    }
}
