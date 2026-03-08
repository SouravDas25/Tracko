package com.trako.controllers;

import com.trako.dtos.TransactionDetailDTO;
import com.trako.dtos.TransactionPeriodSummaryDTO;
import com.trako.dtos.TransactionSummaryDTO;
import com.trako.entities.Account;
import com.trako.models.request.AccountSaveRequest;
import com.trako.repositories.TransactionRepository;
import com.trako.services.AccountService;
import com.trako.services.UserService;
import com.trako.services.transactions.TransactionService;
import com.trako.util.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import com.trako.dtos.TransactionsPageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.trako.util.Response.notFound;

@Tag(name = "Accounts", description = "Manage user accounts")
@RestController
@RequestMapping("/api/accounts")
@Validated
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    private Date getStartOfMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getStartOfNextMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MONTH, 1);
        return calendar.getTime();
    }

    @Operation(summary = "List all accounts for the current user")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Account.class))))
    @GetMapping
    public ResponseEntity<?> getAll() {
        String currentUserId = userService.loggedInUser().getId();
        List<Account> accounts = accountService.findByUserId(currentUserId);
        return Response.ok(accounts);
    }

    @Operation(summary = "Get balances for all accounts (derived from transactions)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "object", description = "Map of accountId to balance")))
    @GetMapping("/balances")
    public ResponseEntity<?> getMyAccountBalances() {
        String currentUserId = userService.loggedInUser().getId();

        // Balance derived purely from transactions:
        // - include countable income/expense (isCountable=1)
        // - include transfers detected via linkedTransactionId != null
        final var rows = transactionRepository.sumBalancesByAccountForUserFromTransactions(currentUserId);
        Map<Long, Double> balances = new HashMap<>();
        if (rows != null) {
            for (var row : rows) {
                Object accountIdObj = row.get("accountId");
                Object balanceObj = row.get("balance");
                if (!(accountIdObj instanceof Number) || !(balanceObj instanceof Number)) {
                    continue;
                }
                balances.put(((Number) accountIdObj).longValue(), ((Number) balanceObj).doubleValue());
            }
        }
        return Response.ok(balances);
    }

    @Operation(summary = "Get an account by ID")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Account.class)))
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable @Positive Long id) {
        String currentUserId = userService.loggedInUser().getId();
        Account account = accountService.findById(id).orElse(null);
        if (account == null) {
            return notFound("Account not found");
        }
        if (!currentUserId.equals(account.getUserId())) {
            return Response.unauthorized();
        }

        Double balance = transactionRepository.sumBalanceForAccountFromTransactions(currentUserId, id);
        account.setBalance(balance == null ? 0.0 : balance);

        return Response.ok(account);
    }

    @Operation(summary = "Get income/expense summary for an account in a date range")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TransactionSummaryDTO.class)))
    @GetMapping("/{id}/summary")
    public ResponseEntity<?> getAccountSummary(
            @PathVariable @Positive Long id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false, defaultValue = "true") boolean includeRollover) {
        String currentUserId = userService.loggedInUser().getId();
        Account account = accountService.findById(id).orElse(null);
        if (account == null) {
            return notFound("Account not found");
        }
        if (!currentUserId.equals(account.getUserId())) {
            return Response.unauthorized();
        }

        TransactionSummaryDTO summary;
        if (includeRollover) {
            summary = transactionService.getAccountSummaryWithRollover(currentUserId, id, startDate, endDate);
        } else {
            summary = transactionService.getAccountSummary(currentUserId, id, startDate, endDate);
        }
        return Response.ok(summary);
    }

    /**
     * GET /api/accounts/{id}/summary/monthly
     * Returns monthly summaries for a specific account.
     */
    @Operation(summary = "Get monthly summaries for an account")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionPeriodSummaryDTO.class))))
    @GetMapping("/{id}/summary/monthly")
    public ResponseEntity<?> getAccountMonthlySummaries(
            @PathVariable @Positive Long id,
            @RequestParam(required = false) Integer year) {
        String currentUserId = userService.loggedInUser().getId();
        Account account = accountService.findById(id).orElse(null);
        if (account == null) {
            return notFound("Account not found");
        }
        if (!currentUserId.equals(account.getUserId())) {
            return Response.unauthorized();
        }

        int resolvedYear = (year == null) ? Calendar.getInstance().get(Calendar.YEAR) : year;
        List<TransactionPeriodSummaryDTO> summaries = transactionService.getMonthlySummaries(currentUserId, resolvedYear, List.of(id), null);
        return Response.ok(summaries);
    }

    /**
     * GET /api/accounts/{id}/summary/yearly
     * Returns yearly summaries for a specific account.
     */
    @Operation(summary = "Get yearly summaries for an account")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionPeriodSummaryDTO.class))))
    @GetMapping("/{id}/summary/yearly")
    public ResponseEntity<?> getAccountYearlySummaries(@PathVariable Long id) {
        // note: validated via annotation below
        return getAccountYearlySummariesValidated(id);
    }

    public ResponseEntity<?> getAccountYearlySummariesValidated(@PathVariable @Positive Long id) {
        String currentUserId = userService.loggedInUser().getId();
        Account account = accountService.findById(id).orElse(null);
        if (account == null) {
            return notFound("Account not found");
        }
        if (!currentUserId.equals(account.getUserId())) {
            return Response.unauthorized();
        }

        List<TransactionPeriodSummaryDTO> summaries = transactionService.getYearlySummaries(currentUserId, List.of(id), null);
        return Response.ok(summaries);
    }

    @Operation(summary = "List transactions for an account with optional filters")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TransactionsPageDTO.class)))
    @GetMapping("/{id}/transactions")
    public ResponseEntity<?> getAccountTransactions(
            @PathVariable @Positive Long id,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "500") Integer size,
            @RequestParam(defaultValue = "false") boolean expand) {
        if (page == null || page < 0) {
            return Response.badRequest("page must be 0 or greater");
        }
        if (size == null || size < 1 || size > 10000) {
            return Response.badRequest("size must be between 1 and 10000");
        }

        String currentUserId = userService.loggedInUser().getId();
        Account account = accountService.findById(id).orElse(null);
        if (account == null) {
            return notFound("Account not found");
        }
        if (!currentUserId.equals(account.getUserId())) {
            return Response.unauthorized();
        }

        Date start;
        Date end;
        int resolvedYear = (year == null) ? Calendar.getInstance().get(Calendar.YEAR) : year;
        if (startDate != null && endDate != null) {
            start = startDate;
            end = endDate;
        } else if (month != null) {
            if (month < 1 || month > 12) {
                return Response.badRequest("month must be between 1 and 12");
            }
            start = getStartOfMonth(resolvedYear, month);
            end = getStartOfNextMonth(resolvedYear, month);
        } else {
            return Response.badRequest("Either month or startDate/endDate must be provided");
        }

        List<Long> accountIds = List.of(id);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id")));

        if (expand) {
            Page<TransactionDetailDTO> dtoPage;
            if (categoryId != null && categoryId > 0) {
                dtoPage = transactionService.findWithDetailsByUserIdAndCategoryIdAndDateBetweenAndAccountIds(
                        currentUserId,
                        categoryId,
                        start,
                        end,
                        accountIds,
                        pageable
                );
            } else {
                dtoPage = transactionService.findWithDetailsByUserIdAndDateBetween(
                        currentUserId,
                        start,
                        end,
                        accountIds,
                        pageable
                );
            }

            List<TransactionDetailDTO> dtos = new ArrayList<>(dtoPage.getContent());

            Map<String, Object> payload = new HashMap<>();
            payload.put("month", month);
            payload.put("year", resolvedYear);
            payload.put("page", dtoPage.getNumber());
            payload.put("size", dtoPage.getSize());
            payload.put("totalElements", dtoPage.getTotalElements());
            payload.put("totalPages", dtoPage.getTotalPages());
            payload.put("hasNext", dtoPage.hasNext());
            payload.put("hasPrevious", dtoPage.hasPrevious());
            payload.put("transactions", dtos);
            return Response.ok(payload);
        }

        Page<com.trako.entities.Transaction> txPage;
        if (categoryId != null && categoryId > 0) {
            txPage = transactionService.findByUserIdAndCategoryIdAndDateBetweenAndAccountIds(
                    currentUserId,
                    categoryId,
                    start,
                    end,
                    accountIds,
                    pageable
            );
        } else {
            txPage = transactionService.findByUserIdAndDateBetweenAndAccountIds(
                    currentUserId,
                    start,
                    end,
                    accountIds,
                    pageable
            );
        }

        List<com.trako.entities.Transaction> transactions = new ArrayList<>(txPage.getContent());

        Map<String, Object> payload = new HashMap<>();
        payload.put("month", month);
        payload.put("year", resolvedYear);
        payload.put("page", txPage.getNumber());
        payload.put("size", txPage.getSize());
        payload.put("totalElements", txPage.getTotalElements());
        payload.put("totalPages", txPage.getTotalPages());
        payload.put("hasNext", txPage.hasNext());
        payload.put("hasPrevious", txPage.hasPrevious());
        payload.put("transactions", transactions);
        return Response.ok(payload);
    }

    @Operation(summary = "Create a new account")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Account.class)))
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

    @Operation(summary = "Update an account")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Account.class)))
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable @Positive Long id, @Valid @RequestBody AccountSaveRequest request) {
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

    @Operation(summary = "Delete an account")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "string")))
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable @Positive Long id) {
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
