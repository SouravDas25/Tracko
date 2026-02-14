package com.trako.controllers;

import com.trako.dtos.BudgetAllocationRequestDTO;
import com.trako.dtos.BudgetCategoryDTO;
import com.trako.dtos.BudgetResponseDTO;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.services.BudgetCalculationService;
import com.trako.services.UserService;
import com.trako.util.Response;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    @Autowired
    private BudgetCalculationService budgetCalculationService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getBudget(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "true") boolean includeActual,
            @RequestParam(defaultValue = "true") boolean includeRollover,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        
        try {
            String userId = userService.loggedInUser().getId();
            
            // Default to current date if not provided
            if (month == null || year == null) {
                LocalDate now = LocalDate.now();
                if (month == null) month = now.getMonthValue();
                if (year == null) year = now.getYear();
            }

            BudgetResponseDTO budgetDetails = budgetCalculationService.getBudgetDetails(
                    userId, month, year, includeActual, includeRollover, categoryId);

            // Handle sorting of categories if requested
            if (sortBy != null && budgetDetails.getCategories() != null) {
                sortCategories(budgetDetails.getCategories(), sortBy, sortOrder);
            }

            return Response.ok(budgetDetails);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        } catch (Exception e) {
            return Response.badRequest(e.getMessage());
        }
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentBudget() {
        return getBudget(null, null, null, true, true, null, "asc");
    }

    @PostMapping("/allocate")
    public ResponseEntity<?> allocateFunds(@Valid @RequestBody BudgetAllocationRequestDTO request) {
        try {
            String userId = userService.loggedInUser().getId();
            BudgetCategoryDTO result = budgetCalculationService.allocateFunds(userId, request);
            return Response.ok(result);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        } catch (Exception e) {
            return Response.badRequest(e.getMessage());
        }
    }

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableToAssign(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        try {
            String userId = userService.loggedInUser().getId();
            
            if (month == null || year == null) {
                LocalDate now = LocalDate.now();
                if (month == null) month = now.getMonthValue();
                if (year == null) year = now.getYear();
            }

            Double available = budgetCalculationService.calculateAvailableToAssign(userId, month, year);
            return Response.ok(available);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        } catch (Exception e) {
            return Response.badRequest(e.getMessage());
        }
    }

    private void sortCategories(List<BudgetCategoryDTO> categories, String sortBy, String sortOrder) {
        Comparator<BudgetCategoryDTO> comparator = null;
        
        switch (sortBy.toLowerCase()) {
            case "name":
            case "category":
                comparator = Comparator.comparing(BudgetCategoryDTO::getCategoryName);
                break;
            case "amount":
            case "allocated":
                comparator = Comparator.comparing(BudgetCategoryDTO::getAllocatedAmount);
                break;
            case "spent":
            case "actual":
                comparator = Comparator.comparing(BudgetCategoryDTO::getActualSpent);
                break;
            case "remaining":
                comparator = Comparator.comparing(BudgetCategoryDTO::getRemainingBalance);
                break;
            default:
                return;
        }

        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }

        categories.sort(comparator);
    }
}
