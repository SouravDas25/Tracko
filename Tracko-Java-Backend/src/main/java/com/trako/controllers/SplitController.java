package com.trako.controllers;

import com.trako.entities.Split;
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
        List<Split> splits = splitService.findByTransactionId(transactionId);
        return Response.ok(splits);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUserId(@PathVariable String userId) {
        List<Split> splits = splitService.findByUserId(userId);
        return Response.ok(splits);
    }

    @GetMapping("/user/{userId}/unsettled")
    public ResponseEntity<?> getUnsettledByUserId(@PathVariable String userId) {
        List<Split> splits = splitService.findUnsettledByUserId(userId);
        return Response.ok(splits);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Split split) {
        Split saved = splitService.save(split);
        return Response.ok(saved, "Split created successfully");
    }

    @PatchMapping("/settle/{splitId}")
    public ResponseEntity<?> settle(@PathVariable Long splitId) {
        splitService.settleSplit(splitId);
        return Response.ok("Split settled successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        splitService.delete(id);
        return Response.ok("Split deleted successfully");
    }
}
