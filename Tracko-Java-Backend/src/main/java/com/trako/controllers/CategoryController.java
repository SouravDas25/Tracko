package com.trako.controllers;

import com.trako.entities.Category;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.models.request.CategorySaveRequest;
import com.trako.services.CategoryService;
import com.trako.services.UserService;
import com.trako.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<Category> categories = categoryService.findAll();
        return Response.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return categoryService.findById(id)
                .map(Response::ok)
                .orElse(Response.notFound("Category not found"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getByUserId(@PathVariable String userId) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            if (!currentUserId.equals(userId)) {
                return Response.unauthorized();
            }
            List<Category> categories = categoryService.findByUserId(currentUserId);
            return Response.ok(categories);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CategorySaveRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        try {
            category.setUserId(userService.loggedInUser().getId());
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
        Category saved = categoryService.save(category);
        return Response.ok(saved, "Category created successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CategorySaveRequest request) {
        Category category = new Category();
        category.setId(id);
        category.setName(request.getName());
        try {
            category.setUserId(userService.loggedInUser().getId());
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
        Category updated = categoryService.save(category);
        return Response.ok(updated, "Category updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Response.ok("Category deleted successfully");
    }
}
