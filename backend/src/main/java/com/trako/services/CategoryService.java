package com.trako.services;

import com.trako.entities.Category;
import com.trako.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

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
        categoryRepository.deleteById(id);
    }
}
