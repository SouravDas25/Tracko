package com.trako.controllers;

import com.trako.dtos.StatsResponseDTO;
import com.trako.entities.TransactionType;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.services.StatsService;
import com.trako.services.UserService;
import com.trako.util.Response;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.Date;

@RestController
@RequestMapping("/api/stats")
@Validated
public class StatsController {

    @Autowired
    private UserService userService;

    @Autowired
    private StatsService statsService;

    @GetMapping("/summary")
    public ResponseEntity<?> getStats(
            @RequestParam @NotBlank String range,
            @RequestParam TransactionType transactionType,
            @RequestParam(required = false) @Positive(message = "Invalid accountId") Long accountId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            Date date,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            Date startDate,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            Date endDate
    ) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            StatsService.Range r;
            try {
                r = StatsService.Range.valueOf(range.toLowerCase());
            } catch (Exception e) {
                return Response.badRequest("Invalid range. Use weekly|monthly|yearly|custom");
            }

            Date anchor = (date == null) ? new Date() : date;
            System.out.println("[StatsController] /api/stats/summary range=" + range
                    + " transactionType=" + transactionType
                    + " accountId=" + accountId
                    + " anchor=" + anchor);

            StatsResponseDTO dto = statsService.getStats(currentUserId, r, transactionType, accountId, anchor, startDate, endDate);
            return Response.ok(dto);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @GetMapping("/category-summary")
    public ResponseEntity<?> getCategoryStats(
            @RequestParam @NotBlank String range,
            @RequestParam TransactionType transactionType,
            @RequestParam @Positive(message = "Invalid categoryId") Long categoryId,
            @RequestParam(required = false) @Positive(message = "Invalid accountId") Long accountId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            Date date,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            Date startDate,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            Date endDate
    ) {
        try {
            var currentUserId = userService.loggedInUser().getId();
            StatsService.Range r;
            try {
                r = StatsService.Range.valueOf(range.toLowerCase());
            } catch (Exception e) {
                return Response.badRequest("Invalid range. Use weekly|monthly|yearly|custom");
            }

            if (categoryId == null || categoryId <= 0) {
                return Response.badRequest("Invalid categoryId");
            }

            Date anchor = (date == null) ? new Date() : date;
            return Response.ok(statsService.getCategoryStats(currentUserId, r, transactionType, accountId, anchor, categoryId, startDate, endDate));
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }
}
