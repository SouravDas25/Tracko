package com.trako.controllers;

import com.trako.entities.Transaction;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.models.request.TransferRequest;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.services.TransactionWriteService;
import com.trako.services.UserService;
import com.trako.util.Response;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private static final int TYPE_DEBIT = 1;
    private static final int TYPE_CREDIT = 2;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionWriteService transactionWriteService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TransferRequest req) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            if (req.getFromAccountId().equals(req.getToAccountId())) {
                return Response.badRequest("fromAccountId and toAccountId cannot be same");
            }
            // validate accounts ownership
            boolean ownsFrom = accountRepository.findByUserId(currentUserId)
                    .stream().anyMatch(a -> a.getId().equals(req.getFromAccountId()));
            boolean ownsTo = accountRepository.findByUserId(currentUserId)
                    .stream().anyMatch(a -> a.getId().equals(req.getToAccountId()));
            if (!ownsFrom || !ownsTo) {
                return Response.unauthorized();
            }
            // find TRANSFER category for user
            var catList = categoryRepository.findByUserIdAndName(currentUserId, "TRANSFER");
            Long transferCategoryId = catList.isEmpty() ? null : catList.get(0).getId();
            if (transferCategoryId == null) {
                // Auto-create TRANSFER category for the user
                var c = new com.trako.entities.Category();
                c.setName("TRANSFER");
                c.setUserId(currentUserId);
                var saved = categoryRepository.save(c);
                transferCategoryId = saved.getId();
            }
            // create DEBIT on source
            Transaction debit = new Transaction();
            debit.setAccountId(req.getFromAccountId());
            debit.setCategoryId(transferCategoryId);
            debit.setTransactionType(TYPE_DEBIT);
            debit.setAmount(req.getAmount());
            debit.setDate(new Date());
            debit.setIsCountable(0);
            debit.setName(req.getName() != null ? req.getName() : "Transfer Out");
            debit.setComments(req.getComments());
            transactionWriteService.saveForUser(currentUserId, debit);
            // create CREDIT on destination
            Transaction credit = new Transaction();
            credit.setAccountId(req.getToAccountId());
            credit.setCategoryId(transferCategoryId);
            credit.setTransactionType(TYPE_CREDIT);
            credit.setAmount(req.getAmount());
            credit.setDate(new Date());
            credit.setIsCountable(0);
            credit.setName(req.getName() != null ? req.getName() : "Transfer In");
            credit.setComments(req.getComments());
            transactionWriteService.saveForUser(currentUserId, credit);

            return Response.ok("Transfer created successfully");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        } catch (Exception e) {
            return Response.badRequest("Transfer failed: " + e.getMessage());
        }
    }
}
