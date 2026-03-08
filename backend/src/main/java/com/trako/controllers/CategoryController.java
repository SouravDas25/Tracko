package com.trako.controllers;

import com.trako.entities.Category;
import com.trako.enums.CategoryType;
import com.trako.models.request.CategorySaveRequest;
import com.trako.services.CategoryService;
import com.trako.services.UserService;
import com.trako.util.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Categories", description = "Manage transaction categories")
@RestController
@RequestMapping("/api/categories")
@Validated
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Operation(summary = "List all categories for the current user")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Category.class))))
    @GetMapping
    public ResponseEntity<?> getAll() {
        String currentUserId = userService.loggedInUser().getId();
        List<Category> categories = categoryService.findByUserId(currentUserId);
        return Response.ok(categories);
    }

    @Operation(summary = "Get a category by ID")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Category.class)))
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

    @Operation(summary = "Create a new category")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Category.class)))
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

    @Operation(summary = "Update a category")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Category.class)))
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

    @Operation(summary = "Delete a category")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "string")))
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
