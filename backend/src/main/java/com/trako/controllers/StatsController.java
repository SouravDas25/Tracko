package com.trako.controllers;

import com.trako.dtos.StatsResponseDTO;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.services.StatsService;
import com.trako.services.UserService;
import com.trako.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Date;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private UserService userService;

    @Autowired
    private StatsService statsService;

    @GetMapping("/summary")
    public ResponseEntity<?> getStats(
            @RequestParam String range,
            @RequestParam Integer transactionType,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            Date date
    ) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            StatsService.Range r;
            try {
                r = StatsService.Range.valueOf(range.toLowerCase());
            } catch (Exception e) {
                return Response.badRequest("Invalid range. Use weekly|monthly|yearly");
            }

            if (transactionType == null || (transactionType != 1 && transactionType != 2)) {
                return Response.badRequest("Invalid transactionType. Use 1 (DEBIT) or 2 (CREDIT)");
            }

            Date anchor = (date == null) ? new Date() : date;
            System.out.println("[StatsController] /api/stats/summary range=" + range
                    + " transactionType=" + transactionType
                    + " anchor=" + anchor);

            StatsResponseDTO dto = statsService.getStats(currentUserId, r, transactionType, anchor);
            return Response.ok(dto);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @GetMapping("/category-summary")
    public ResponseEntity<?> getCategoryStats(
            @RequestParam String range,
            @RequestParam Integer transactionType,
            @RequestParam Long categoryId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            Date date
    ) {
        try {
            var currentUserId = userService.loggedInUser().getId();
            StatsService.Range r;
            try {
                r = StatsService.Range.valueOf(range.toLowerCase());
            } catch (Exception e) {
                return Response.badRequest("Invalid range. Use weekly|monthly|yearly");
            }

            if (transactionType == null || (transactionType != 1 && transactionType != 2)) {
                return Response.badRequest("Invalid transactionType. Use 1 (DEBIT) or 2 (CREDIT)");
            }

            if (categoryId == null || categoryId <= 0) {
                return Response.badRequest("Invalid categoryId");
            }

            Date anchor = (date == null) ? new Date() : date;
            return Response.ok(statsService.getCategoryStats(currentUserId, r, transactionType, anchor, categoryId));
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }
}
