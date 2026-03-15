package com.trako.controllers;

import com.trako.dtos.AnalyticsResponseDTO;
import com.trako.enums.AnalyticsGranularity;
import com.trako.enums.AnalyticsGroupBy;
import com.trako.enums.TransactionType;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.services.AnalyticsService;
import com.trako.services.UserService;
import com.trako.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@Tag(name = "Analytics", description = "Analytics chart data with grouping and granularity controls")
@RestController
@RequestMapping("/api/analytics")
@Validated
public class AnalyticsController {

    @Autowired
    private UserService userService;

    @Autowired
    private AnalyticsService analyticsService;

    @Operation(summary = "Get chart data with optional grouping and granularity")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = AnalyticsResponseDTO.class)))
    @GetMapping("/chart")
    public ResponseEntity<?> getChartData(
            @RequestParam TransactionType transactionType,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) String granularity,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId
    ) {
        try {
            String currentUserId = userService.loggedInUser().getId();

            // Validate groupBy
            AnalyticsGroupBy groupByEnum = null;
            if (groupBy != null && !groupBy.isEmpty()) {
                groupByEnum = AnalyticsGroupBy.fromString(groupBy);
                if (groupByEnum == null) {
                    return Response.badRequest("Invalid groupBy. Use category or account");
                }
            }

            // Validate granularity
            AnalyticsGranularity granularityEnum = AnalyticsGranularity.fromString(granularity);
            if (granularity != null && !granularity.isEmpty() && granularityEnum == null) {
                return Response.badRequest("Invalid granularity. Use weekly, monthly, or yearly");
            }

            List<Long> accountIds = accountId != null ? List.of(accountId) : null;
            List<Long> categoryIds = categoryId != null ? List.of(categoryId) : null;

            AnalyticsResponseDTO dto = analyticsService.getChartData(
                    currentUserId, transactionType, startDate, endDate,
                    granularityEnum, groupByEnum, accountIds, categoryIds
            );
            return Response.ok(dto);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }
}
