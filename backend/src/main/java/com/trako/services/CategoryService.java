package com.trako.services;

import com.trako.entities.Category;
import com.trako.repositories.BudgetCategoryAllocationRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.RecurringTransactionRepository;
import com.trako.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetCategoryAllocationRepository budgetCategoryAllocationRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    public List<Category> findByUserId(String userId) {
        return categoryRepository.findByUserIdOrderByNameAsc(userId);
    }

    public Category save(Category category) {
        if (category.getName() != null) {
            category.setName(category.getName().trim());
        }
        if (category.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (category.getName() == null || category.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        boolean duplicate;
        if (category.getId() == null) {
            duplicate = categoryRepository.existsByUserIdAndNameIgnoreCase(category.getUserId(), category.getName());
        } else {
            duplicate = categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(category.getUserId(), category.getName(), category.getId());
        }
        if (duplicate) {
            throw new IllegalArgumentException("Category name already exists");
        }
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        // Prevent deletion if transactions or budget allocations exist for this category
        if (transactionRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("Cannot delete category: Transactions exist. Reassign or delete those transactions first.");
        }
        if (budgetCategoryAllocationRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("Cannot delete category: Budget allocations exist. Remove allocations first.");
        }
        if (recurringTransactionRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("Cannot delete category: Recurring transactions reference this category. Update or delete them first.");
        }
        categoryRepository.deleteById(id);
    }
}
