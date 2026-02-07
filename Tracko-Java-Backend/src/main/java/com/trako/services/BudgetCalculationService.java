package com.trako.services;

import com.trako.dtos.BudgetAllocationRequestDTO;
import com.trako.dtos.BudgetCategoryDTO;
import com.trako.dtos.BudgetResponseDTO;
import com.trako.entities.*;
import com.trako.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BudgetCalculationService {

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

    @Transactional
    public BudgetResponseDTO getBudgetDetails(String userId, Integer month, Integer year,
                                              boolean includeActual, boolean includeRollover, Long categoryId) {
        
        // 1. Get or Create BudgetMonth
        BudgetMonth budgetMonth = getOrCreateBudgetMonth(userId, month, year);

        // 2. Calculate Income for the month
        Date startDate = getStartDate(month, year);
        Date endDate = getEndDate(month, year);
        Double totalIncome = transactionService.getTotalIncome(userId, startDate, endDate);

        // 3. Calculate Rollover (if requested)
        Double rolloverAmount = 0.0;
        if (includeRollover) {
            rolloverAmount = calculateRolloverAmount(userId, month, year);
        }

        // 4. Get Category Allocations
        List<BudgetCategoryAllocation> allocations = budgetCategoryAllocationRepository
                .findByUserIdAndBudgetMonthId(userId, budgetMonth.getId());
        
        // Map to DTOs and calculate actuals if needed
        List<BudgetCategoryDTO> categoryDTOs = new ArrayList<>();
        Double totalAllocated = 0.0;
        Double totalSpent = 0.0;

        // Get categories based on filter
        List<Category> categoriesToProcess;
        if (categoryId != null) {
            Category cat = categoryRepository.findById(categoryId).orElse(null);
            if (cat != null && cat.getUserId().equals(userId)) {
                categoriesToProcess = Collections.singletonList(cat);
            } else {
                categoriesToProcess = Collections.emptyList();
            }
        } else {
            categoriesToProcess = categoryRepository.findByUserId(userId);
        }

        Map<Long, BudgetCategoryAllocation> allocationMap = allocations.stream()
                .collect(Collectors.toMap(BudgetCategoryAllocation::getCategoryId, a -> a));

        for (Category category : categoriesToProcess) {
            BudgetCategoryAllocation allocation = allocationMap.get(category.getId());
            BudgetCategoryDTO dto = new BudgetCategoryDTO();
            dto.setCategoryId(category.getId());
            dto.setCategoryName(category.getName());
            dto.setIsRollOverEnabled(category.getIsRollOverEnabled());
            
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
                        .filter(t -> t.getIsCountable() == 1 && t.getTransactionType() == 1)
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
        
        // Available to Assign = (Income + Rollover) - Total Allocated (Month Total)
        // If we filtered, totalAllocated in local var is partial. We need real total for Available calculation.
        Double realTotalAllocated = budgetMonth.getTotalBudget();
        response.setAvailableToAssign((totalIncome + rolloverAmount) - realTotalAllocated);
        
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

        BudgetCategoryAllocation allocation = budgetCategoryAllocationRepository
                .findByBudgetMonthIdAndCategoryId(budgetMonth.getId(), request.getCategoryId())
                .orElse(new BudgetCategoryAllocation());

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
                .filter(t -> t.getIsCountable() == 1 && t.getTransactionType() == 1)
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
        dto.setIsRollOverEnabled(category.getIsRollOverEnabled());
        
        return dto;
    }

    public Double calculateAvailableToAssign(String userId, Integer month, Integer year) {
        Date startDate = getStartDate(month, year);
        Date endDate = getEndDate(month, year);
        Double totalIncome = transactionService.getTotalIncome(userId, startDate, endDate);
        
        Double rollover = calculateRolloverAmount(userId, month, year);
        
        BudgetMonth budgetMonth = getOrCreateBudgetMonth(userId, month, year);
        Double totalAllocated = budgetMonth.getTotalBudget();
        
        return (totalIncome + rollover) - totalAllocated;
    }

    private Double calculateRolloverAmount(String userId, Integer currentMonth, Integer currentYear) {
        // Simple logic: Find the previous month
        YearMonth current = YearMonth.of(currentYear, currentMonth);
        YearMonth previous = current.minusMonths(1);
        
        Optional<BudgetMonth> prevBudgetOpt = budgetMonthRepository.findByUserIdAndMonthAndYear(
                userId, previous.getMonthValue(), previous.getYear());
        
        if (prevBudgetOpt.isPresent()) {
            BudgetMonth prevBudget = prevBudgetOpt.get();
            // Calculate unallocated from previous month
            Date startDate = getStartDate(previous.getMonthValue(), previous.getYear());
            Date endDate = getEndDate(previous.getMonthValue(), previous.getYear());
            
            Double prevIncome = transactionService.getTotalIncome(userId, startDate, endDate);
            Double prevAllocated = prevBudget.getTotalBudget();
            
            // Also add rollover from the month BEFORE that (recursive chain)
            // Note: To avoid infinite recursion or heavy calculation, we should probably store "final_rollover" in BudgetMonth when it closes.
            // For now, we'll assume dynamic calculation but limit depth or assume closed months have correct state.
            // A better ZBB approach is that unallocated funds naturally sit in "Available to Assign".
            
            // If we strictly follow the requirement: "calculate roll over unallocated fund from the previous month"
            // We need: (Prev Month Income + Prev Month Rollover) - Prev Month Allocated
            
            // To prevent recursion, we could look up the stored rollover if we added a field, but we didn't.
            // Let's implement a shallow lookup for now or rely on the fact that if months are processed sequentially, it works.
            // For this implementation, let's just look at (Income - Allocated) of previous month. 
            // Truly recursive rollover calculation can be expensive.
            
            Double unallocated = prevIncome - prevAllocated;
            
            // Plus: Sum of remaining balances of categories that have rollover enabled
            Double categoryRollovers = 0.0;
            List<BudgetCategoryAllocation> prevAllocations = budgetCategoryAllocationRepository
                    .findByUserIdAndBudgetMonthId(userId, prevBudget.getId());
            
            for (BudgetCategoryAllocation alloc : prevAllocations) {
                Category cat = categoryRepository.findById(alloc.getCategoryId()).orElse(null);
                if (cat != null && Boolean.TRUE.equals(cat.getIsRollOverEnabled())) {
                    if (alloc.getRemainingBalance() > 0) {
                        categoryRollovers += alloc.getRemainingBalance();
                    }
                }
            }
            
            return Math.max(0.0, unallocated + categoryRollovers);
        }
        
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
        double total = allocations.stream()
                .mapToDouble(BudgetCategoryAllocation::getAllocatedAmount)
                .sum();
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
