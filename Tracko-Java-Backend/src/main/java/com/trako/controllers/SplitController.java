package com.trako.controllers;

import com.trako.entities.Split;
import com.trako.entities.Transaction;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.repositories.TransactionRepository;
import com.trako.services.UserService;
import com.trako.services.SplitService;
import com.trako.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/splits")
public class SplitController {

    @Autowired
    private SplitService splitService;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository transactionRepository;

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<Split> splits = splitService.findAll();
        return Response.ok(splits);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return splitService.findById(id)
                .map(Response::ok)
                .orElse(Response.notFound("Split not found"));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<?> getByTransactionId(@PathVariable Long transactionId) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            Transaction tx = transactionRepository.findById(transactionId).orElse(null);
            if (tx == null) {
                return Response.notFound("Transaction not found");
            }
            // TransactionRepository resolves userId via account ownership
            boolean owned = transactionRepository.findByUserId(currentUserId)
                    .stream()
                    .anyMatch(t -> t.getId().equals(transactionId));
            if (!owned) {
                return Response.unauthorized();
            }
            List<Split> splits = splitService.findByTransactionId(transactionId);
            return Response.ok(splits);
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
            List<Split> splits = splitService.findByUserId(currentUserId);
            return Response.ok(splits);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @GetMapping("/user/{userId}/unsettled")
    public ResponseEntity<?> getUnsettledByUserId(@PathVariable String userId) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            if (!currentUserId.equals(userId)) {
                return Response.unauthorized();
            }
            List<Split> splits = splitService.findUnsettledByUserId(currentUserId);
            return Response.ok(splits);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Split split) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            Long txId = split.getTransactionId();
            if (txId == null) {
                return Response.badRequest("transactionId required");
            }
            boolean owned = transactionRepository.findByUserId(currentUserId)
                    .stream()
                    .anyMatch(t -> t.getId().equals(txId));
            if (!owned) {
                return Response.unauthorized();
            }
            Split saved = splitService.save(split);
            return Response.ok(saved, "Split created successfully");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @PatchMapping("/settle/{splitId}")
    public ResponseEntity<?> settle(@PathVariable Long splitId) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            Split split = splitService.findById(splitId).orElse(null);
            if (split == null) {
                return Response.notFound("Split not found");
            }
            boolean owned = transactionRepository.findByUserId(currentUserId)
                    .stream()
                    .anyMatch(t -> t.getId().equals(split.getTransactionId()));
            if (!owned) {
                return Response.unauthorized();
            }
            splitService.settleSplit(splitId);
            return Response.ok("Split settled successfully");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            Split split = splitService.findById(id).orElse(null);
            if (split == null) {
                return Response.notFound("Split not found");
            }
            boolean owned = transactionRepository.findByUserId(currentUserId)
                    .stream()
                    .anyMatch(t -> t.getId().equals(split.getTransactionId()));
            if (!owned) {
                return Response.unauthorized();
            }
            splitService.delete(id);
            return Response.ok("Split deleted successfully");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }
}
