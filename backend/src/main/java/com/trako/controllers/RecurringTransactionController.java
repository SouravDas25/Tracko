package com.trako.controllers;

import com.trako.entities.RecurringTransaction;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.services.RecurringTransactionService;
import com.trako.services.UserService;
import com.trako.util.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
@Validated
public class RecurringTransactionController {

    @Autowired
    private RecurringTransactionService recurringTransactionService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            String currentUserId = userService.loggedInUser().getId();
            List<RecurringTransaction> list = recurringTransactionService.getAll(currentUserId);
            return Response.ok(list);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable @Positive Long id) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            RecurringTransaction rt = recurringTransactionService.getById(id).orElse(null);

            if (rt == null) {
                return Response.notFound("Recurring transaction not found");
            }
            if (!rt.getUserId().equals(currentUserId)) {
                return Response.unauthorized();
            }

            return Response.ok(rt);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody RecurringTransaction recurringTransaction) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            RecurringTransaction created = recurringTransactionService.create(currentUserId, recurringTransaction);
            return Response.ok(created, "Recurring transaction created successfully");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        } catch (Exception e) {
            return Response.badRequest(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable @Positive Long id, @Valid @RequestBody RecurringTransaction updates) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            RecurringTransaction updated = recurringTransactionService.update(currentUserId, id, updates);
            return Response.ok(updated, "Recurring transaction updated successfully");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        } catch (IllegalArgumentException e) {
            if (e.getMessage().equals("Unauthorized")) {
                return Response.unauthorized();
            }
            return Response.badRequest(e.getMessage());
        } catch (Exception e) {
            return Response.badRequest(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable @Positive Long id) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            recurringTransactionService.delete(currentUserId, id);
            return Response.ok("Recurring transaction deleted successfully");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        } catch (IllegalArgumentException e) {
            if (e.getMessage().equals("Unauthorized")) {
                return Response.unauthorized();
            }
            return Response.badRequest(e.getMessage());
        } catch (Exception e) {
            return Response.badRequest(e.getMessage());
        }
    }
}
