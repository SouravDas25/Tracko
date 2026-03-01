package com.trako.services;

import com.trako.dtos.BudgetAllocationRequestDTO;
import com.trako.dtos.BudgetCategoryDTO;
import com.trako.dtos.BudgetResponseDTO;
import com.trako.dtos.TransactionSummaryDTO;
import com.trako.entities.*;
import com.trako.entities.TransactionType;
import com.trako.repositories.BudgetCategoryAllocationRepository;
import com.trako.repositories.BudgetMonthRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BudgetCalculationService {

    private static final CategoryType CATEGORY_TYPE_EXPENSE = CategoryType.EXPENSE;

    @Autowired
    private BudgetMonthRepository budgetMonthRepository;

    @Autowired
    private BudgetCategoryAllocationRepository budgetCategoryAllocationRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionService transactionService;

    private boolean isBudgetableExpenseCategory(Category category) {
        if (category == null) return false;
        if (!CATEGORY_TYPE_EXPENSE.equals(category.getCategoryType())) return false;
        return true;
    }

    @Transactional
    public BudgetResponseDTO getBudgetDetails(String userId, Integer month, Integer year,
                                              boolean includeActual, Long categoryId) {
        
        // 1. Get or Create BudgetMonth
        BudgetMonth budgetMonth = getOrCreateBudgetMonth(userId, month, year);

        // 2. Calculate Income and Opening Balance (Rollover Net)
        Date startDate = getStartDate(month, year);
        Date endDate = getEndDate(month, year);
        
        // We use getSummaryWithRollover to get the Opening Balance (rolloverNet) which is the account balance before this month.
        // Net Total before this month = (Total Income < Month) - (Total Expense < Month)
        TransactionSummaryDTO summary = transactionService.getSummaryWithRollover(userId, startDate, endDate, null);
        
        Double totalIncome = summary.getTotalIncome(); // Income in this month
        Double openingBalance = summary.getRolloverNet(); // Balance at start of month
        
        // 3. Rollover for UI = Previous month's ending balance = Opening balance of current month
        Double rolloverAmount = openingBalance;

        // 4. Get Category Allocations
        List<BudgetCategoryAllocation> allocations = budgetCategoryAllocationRepository
                .findByUserIdAndBudgetMonthId(userId, budgetMonth.getId());

        // Ensure budget month totals reflect EXPENSE-only allocations
        updateMonthTotalBudget(budgetMonth);
        
        // Map to DTOs and calculate actuals if needed
        List<BudgetCategoryDTO> categoryDTOs = new ArrayList<>();
        Double totalAllocated = 0.0;
        Double totalSpent = 0.0;

        // Get categories based on filter
        List<Category> categoriesToProcess;
        if (categoryId != null) {
            Category cat = categoryRepository.findById(categoryId).orElse(null);
            if (cat != null
                    && cat.getUserId().equals(userId)
                    && isBudgetableExpenseCategory(cat)) {
                categoriesToProcess = Collections.singletonList(cat);
            } else {
                categoriesToProcess = Collections.emptyList();
            }
        } else {
            categoriesToProcess = categoryRepository.findByUserIdAndCategoryTypeOrderByNameAsc(userId, CATEGORY_TYPE_EXPENSE)
                    .stream()
                    .filter(this::isBudgetableExpenseCategory)
                    .collect(Collectors.toList());
        }

        Map<Long, BudgetCategoryAllocation> allocationMap = allocations.stream()
                .collect(Collectors.toMap(BudgetCategoryAllocation::getCategoryId, a -> a));

        for (Category category : categoriesToProcess) {
            BudgetCategoryAllocation allocation = allocationMap.get(category.getId());
            BudgetCategoryDTO dto = new BudgetCategoryDTO();
            dto.setCategoryId(category.getId());
            dto.setCategoryName(category.getName());
            
            Double allocated = allocation != null ? allocation.getAllocatedAmount() : 0.0;
            dto.setAllocatedAmount(allocated);
            totalAllocated += allocated; // Note: If filtered, this total reflects only filtered categories.
                                         // If we want total budget for the MONTH regardless of filter, we should use budgetMonth.getTotalBudget()

            Double actual = 0.0;
            if (includeActual) {
                // Calculate actual spending for this category in this month
                List<Transaction> transactions = transactionRepository.findByUserIdAndCategoryIdAndDateBetween(
                        userId, category.getId(), startDate, endDate);
                
                // Filter for expense transactions (type 1)
                actual = transactions.stream()
                        .filter(t -> t.getIsCountable() == 1 && t.getTransactionType() == TransactionType.DEBIT)
                        .mapToDouble(Transaction::getAmount)
                        .sum();
                
                // Update the allocation entity with actuals if it exists
                if (allocation != null) {
                    allocation.setActualSpent(actual);
                    budgetCategoryAllocationRepository.save(allocation);
                }
            } else if (allocation != null) {
                actual = allocation.getActualSpent();
            }
            
            dto.setActualSpent(actual);
            dto.setRemainingBalance(allocated - actual);
            totalSpent += actual;
            
            categoryDTOs.add(dto);
        }

        // 5. Update BudgetMonth totals - ONLY if not filtering, otherwise we might overwrite with partial data
        // Or better: We should calculate totals from DB allocations separately if we want accurate month totals while filtering categories
        
        // If we are filtering, we probably shouldn't update the BudgetMonth total based on the filtered list.
        // But the previous implementation updated it.
        // Let's rely on the separate method updateMonthTotalBudget for consistency, or only update if categoryId is null.
        if (categoryId == null) {
            budgetMonth.setTotalBudget(totalAllocated);
            budgetMonthRepository.save(budgetMonth);
        } else {
            // If filtering, reload totalAllocated from DB to show correct month context
            totalAllocated = budgetMonth.getTotalBudget();
            // Recalculate if we suspect it's stale? For now use stored.
        }

        // 6. Build Response
        BudgetResponseDTO response = new BudgetResponseDTO();
        response.setMonth(month);
        response.setYear(year);
        response.setTotalBudget(totalAllocated);
        response.setTotalIncome(totalIncome);
        // If filtering, totalSpent should probably be for the month or the filter?
        // Usually context values (income, total budget) are for the month, but 'totalSpent' might be ambiguous.
        // Let's keep it consistent: Context is Month, 'categories' list is filtered.
        // So totalSpent should probably be total for the month if we want to show "Budget vs Actual" for the whole month.
        // But if I request a category, I might just care about that category.
        // Let's stick to: totals in the root object are for the MONTH.
        if (categoryId != null) {
            // We need to fetch total spent for the whole month if we want to report it correctly
            // Or we just report what we summed up (which is partial).
            // Given the DTO structure, users might expect totals to match the list.
            // Let's set the totals to match the filtered list for now to avoid confusion, 
            // OR if the user wants context, they request without filter.
            response.setTotalSpent(totalSpent); // This will be partial if filtered
            response.setTotalBudget(categoryDTOs.stream().mapToDouble(BudgetCategoryDTO::getAllocatedAmount).sum()); // Partial budget
        } else {
            response.setTotalSpent(totalSpent);
            response.setTotalBudget(totalAllocated);
        }
        
        response.setRolloverAmount(rolloverAmount);
        
        // Available to Assign = (Opening Balance + Income) - Total Allocated
        Double realTotalAllocated = budgetMonth.getTotalBudget();
        response.setAvailableToAssign((openingBalance + totalIncome) - realTotalAllocated);
        
        response.setIsClosed(budgetMonth.getIsClosed());
        response.setCategories(categoryDTOs);

        return response;
    }

    @Transactional
    public BudgetCategoryDTO allocateFunds(String userId, BudgetAllocationRequestDTO request) {
        BudgetMonth budgetMonth = getOrCreateBudgetMonth(userId, request.getMonth(), request.getYear());
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to category");
        }

        if (!isBudgetableExpenseCategory(category)) {
            throw new IllegalArgumentException("Budgeting is only supported for EXPENSE categories");
        }

        BudgetCategoryAllocation allocation = budgetCategoryAllocationRepository
                .findByBudgetMonthIdAndCategoryId(budgetMonth.getId(), request.getCategoryId())
                .orElse(new BudgetCategoryAllocation());

        // Simplification: Removed Over-Allocation Validation
        // User is allowed to allocate more than available funds.
        
        if (allocation.getId() == null) {
            allocation.setBudgetMonthId(budgetMonth.getId());
            allocation.setCategoryId(request.getCategoryId());
            allocation.setUserId(userId);
            allocation.setActualSpent(0.0);
        }

        allocation.setAllocatedAmount(request.getAmount());
        
        // Recalculate remaining based on actuals if we have them, strictly speaking we should probably re-fetch actuals here 
        // but for performance we rely on the last known state or 0. In a real real-time system we might want to fetch.
        // Let's fetch actuals to be safe and accurate.
        Date startDate = getStartDate(request.getMonth(), request.getYear());
        Date endDate = getEndDate(request.getMonth(), request.getYear());
        List<Transaction> transactions = transactionRepository.findByUserIdAndCategoryIdAndDateBetween(
                userId, category.getId(), startDate, endDate);
        Double actual = transactions.stream()
                .filter(t -> t.getIsCountable() == 1 && t.getTransactionType() == TransactionType.DEBIT)
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        allocation.setActualSpent(actual);
        
        BudgetCategoryAllocation saved = budgetCategoryAllocationRepository.save(allocation);

        // Update total budget in month
        updateMonthTotalBudget(budgetMonth);

        BudgetCategoryDTO dto = new BudgetCategoryDTO();
        dto.setCategoryId(saved.getCategoryId());
        dto.setCategoryName(category.getName());
        dto.setAllocatedAmount(saved.getAllocatedAmount());
        dto.setActualSpent(saved.getActualSpent());
        dto.setRemainingBalance(saved.getRemainingBalance());
        
        return dto;
    }

    public Double calculateAvailableToAssign(String userId, Integer month, Integer year) {
        Date startDate = getStartDate(month, year);
        Date endDate = getEndDate(month, year);
        
        TransactionSummaryDTO summary = transactionService.getSummaryWithRollover(userId, startDate, endDate, null);
        
        Double totalIncome = summary.getTotalIncome();
        Double openingBalance = summary.getRolloverNet();
        
        BudgetMonth budgetMonth = getOrCreateBudgetMonth(userId, month, year);
        Double totalAllocated = budgetMonth.getTotalBudget();
        
        return (openingBalance + totalIncome) - totalAllocated;
    }

    public Double calculateRolloverAmount(String userId, Integer currentMonth, Integer currentYear) {
        // Deprecated in simplified model; retained for compatibility. Always return 0.0.
        return 0.0;
    }

    private BudgetMonth getOrCreateBudgetMonth(String userId, Integer month, Integer year) {
        return budgetMonthRepository.findByUserIdAndMonthAndYear(userId, month, year)
                .orElseGet(() -> {
                    BudgetMonth newMonth = new BudgetMonth();
                    newMonth.setUserId(userId);
                    newMonth.setMonth(month);
                    newMonth.setYear(year);
                    newMonth.setTotalBudget(0.0);
                    newMonth.setIsClosed(false);
                    return budgetMonthRepository.save(newMonth);
                });
    }

    private void updateMonthTotalBudget(BudgetMonth budgetMonth) {
        List<BudgetCategoryAllocation> allocations = budgetCategoryAllocationRepository
                .findByBudgetMonthId(budgetMonth.getId());

        double total = 0.0;
        for (BudgetCategoryAllocation alloc : allocations) {
            Category cat = categoryRepository.findById(alloc.getCategoryId()).orElse(null);
            if (cat == null) continue;
            if (!isBudgetableExpenseCategory(cat)) continue;
            total += alloc.getAllocatedAmount() != null ? alloc.getAllocatedAmount() : 0.0;
        }

        budgetMonth.setTotalBudget(total);
        budgetMonthRepository.save(budgetMonth);
    }

    private Date getStartDate(Integer month, Integer year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getEndDate(Integer month, Integer year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MONTH, 1);
        return calendar.getTime();
    }
}
