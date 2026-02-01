package com.trako.controllers;

import com.trako.entities.Account;
import com.trako.models.request.AccountSaveRequest;
import com.trako.services.AccountService;
import com.trako.services.UserService;
import com.trako.util.Response;
import com.trako.exceptions.UserNotLoggedInException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

import static com.trako.util.Response.notFound;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<Account> accounts = accountService.findAll();
        return Response.ok(accounts);
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
