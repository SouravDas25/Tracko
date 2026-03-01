package com.trako.controllers;

import com.trako.entities.Split;
import com.trako.entities.Transaction;
import com.trako.repositories.ContactRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.services.SplitService;
import com.trako.services.UserService;
import com.trako.util.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Controller for managing expense splits.
 *
 * <p>The Split feature allows users to divide a transaction amount among multiple contacts.
 * This is useful for tracking shared expenses where one person pays and others owe them.
 *
 * <p>Key concepts:
 * <ul>
 *   <li><b>Creation:</b> A split is created for a specific transaction and assigned to a contact.
 *       It represents the amount that contact owes the user.</li>
 *   <li><b>Settlement:</b> When a contact pays back their share, the split is marked as "settled".
 *       IMPORTANT: Settling a split does NOT automatically create a transaction in the system.
 *       This is by design because the repayment could come in various forms (cash, bank transfer, etc.)
 *       and to different accounts. The user must manually create an income transaction to record
 *       the actual receipt of funds if they wish to track it in their accounts.</li>
 *   <li><b>Unsettlement:</b> If a settlement was marked in error, it can be reversed.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/splits")
@Validated
public class SplitController {

    @Autowired
    private SplitService splitService;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ContactRepository contactRepository;

    @GetMapping
    public ResponseEntity<?> getAll() {
        String currentUserId = userService.loggedInUser().getId();
        List<Split> splits = splitService.findByUserId(currentUserId);
        return Response.ok(splits);
    }

    @GetMapping("/contact/{contactId}")
    public ResponseEntity<?> getByContactId(@PathVariable Long contactId) {
        return getByContactIdValidated(contactId);
    }

    public ResponseEntity<?> getByContactIdValidated(@PathVariable @Positive Long contactId) {
        String currentUserId = userService.loggedInUser().getId();
        var contactOpt = contactRepository.findById(contactId);
        if (contactOpt.isEmpty() || !currentUserId.equals(contactOpt.get().getUserId())) {
            return Response.unauthorized();
        }
        List<Split> splits = splitService.findByContactId(contactId);
        return Response.ok(splits);
    }

    @GetMapping("/contact/{contactId}/unsettled")
    public ResponseEntity<?> getUnsettledByContactId(@PathVariable Long contactId) {
        return getUnsettledByContactIdValidated(contactId);
    }

    public ResponseEntity<?> getUnsettledByContactIdValidated(@PathVariable @Positive Long contactId) {
        String currentUserId = userService.loggedInUser().getId();
        var contactOpt = contactRepository.findById(contactId);
        if (contactOpt.isEmpty() || !currentUserId.equals(contactOpt.get().getUserId())) {
            return Response.unauthorized();
        }
        List<Split> splits = splitService.findUnsettledByContactId(contactId);
        return Response.ok(splits);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable @Positive Long id) {
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
        return Response.ok(split);
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<?> getByTransactionId(@PathVariable @Positive Long transactionId) {
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
    }

    @GetMapping("/unsettled")
    public ResponseEntity<?> getMyUnsettled() {
        String currentUserId = userService.loggedInUser().getId();
        List<Split> splits = splitService.findUnsettledByUserId(currentUserId);
        return Response.ok(splits);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Split split) {
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
        // Validate contact ownership if provided
        if (split.getContactId() != null) {
            var contactOpt = contactRepository.findById(split.getContactId());
            if (contactOpt.isEmpty() || !currentUserId.equals(contactOpt.get().getUserId())) {
                return Response.unauthorized();
            }
        }
        // Ensure split is associated to current owner
        split.setUserId(currentUserId);
        Split saved = splitService.save(split);
        return Response.ok(saved, "Split created successfully");
    }

    @PatchMapping("/settle/{splitId}")
    public ResponseEntity<?> settle(@PathVariable @Positive Long splitId) {
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
    }

    @PatchMapping("/unsettle/{splitId}")
    public ResponseEntity<?> unsettle(@PathVariable @Positive Long splitId) {
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
        splitService.unsettleSplit(splitId);
        return Response.ok("Split unsettled successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable @Positive Long id) {
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
    }
}
