package com.trako.controllers;

import com.trako.entities.Category;
import com.trako.entities.CategoryType;
import com.trako.models.request.CategorySaveRequest;
import com.trako.services.CategoryService;
import com.trako.services.UserService;
import com.trako.util.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Validated
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        String currentUserId = userService.loggedInUser().getId();
        List<Category> categories = categoryService.findByUserId(currentUserId);
        return Response.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable @Positive Long id) {
        String currentUserId = userService.loggedInUser().getId();
        Category category = categoryService.findById(id).orElse(null);
        if (category == null) {
            return Response.notFound("Category not found");
        }
        if (!currentUserId.equals(category.getUserId())) {
            return Response.unauthorized();
        }
        return Response.ok(category);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUserId(@PathVariable @NotBlank String userId) {
        String currentUserId = userService.loggedInUser().getId();
        if (!currentUserId.equals(userId)) {
            return Response.unauthorized();
        }
        List<Category> categories = categoryService.findByUserId(currentUserId);
        return Response.ok(categories);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CategorySaveRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        CategoryType type = request.getCategoryType() != null ? request.getCategoryType() : CategoryType.EXPENSE;
        category.setCategoryType(type);
        category.setUserId(userService.loggedInUser().getId());
        Category saved = categoryService.save(category);
        return Response.ok(saved, "Category created successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable @Positive Long id, @Valid @RequestBody CategorySaveRequest request) {
        Category category = categoryService.findById(id)
                .orElse(null);

        if (category == null) {
            return Response.notFound("Category not found");
        }

        category.setName(request.getName());
        if (request.getCategoryType() != null) {
            category.setCategoryType(request.getCategoryType());
        }
        String currentUserId = userService.loggedInUser().getId();
        if (!currentUserId.equals(category.getUserId())) {
            return Response.unauthorized();
        }
        Category updated = categoryService.save(category);
        return Response.ok(updated, "Category updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable @Positive Long id) {
        String currentUserId = userService.loggedInUser().getId();
        Category category = categoryService.findById(id).orElse(null);
        if (category == null) {
            return Response.notFound("Category not found");
        }
        if (!currentUserId.equals(category.getUserId())) {
            return Response.unauthorized();
        }
        categoryService.delete(id);
        return Response.ok("Category deleted successfully");
    }
}
