package com.trako.controllers;

import com.trako.entities.Category;
import com.trako.services.CategoryService;
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
        List<Category> categories = categoryService.findByUserId(userId);
        return Response.ok(categories);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Category category) {
        Category saved = categoryService.save(category);
        return Response.ok(saved, "Category created successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Category category) {
        category.setId(id);
        Category updated = categoryService.save(category);
        return Response.ok(updated, "Category updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Response.ok("Category deleted successfully");
    }
}
