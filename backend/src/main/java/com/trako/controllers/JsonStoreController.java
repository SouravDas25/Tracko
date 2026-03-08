package com.trako.controllers;

import com.trako.entities.JsonStore;
import com.trako.services.JsonStoreService;
import com.trako.services.UserService;
import com.trako.util.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

@Tag(name = "JSON Store", description = "Generic key-value JSON storage for app settings")
@RestController
@RequestMapping("/api/json-store")
@Validated
public class JsonStoreController {

    @Autowired
    private JsonStoreService jsonStoreService;

    @Autowired
    private UserService userService;

    @Operation(summary = "List all JSON store entries")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = JsonStore.class))))
    @GetMapping
    public ResponseEntity<?> getAll() {
        userService.loggedInUser();
        List<JsonStore> items = jsonStoreService.findAll();
        return Response.ok(items);
    }

    @Operation(summary = "Get a JSON store entry by name")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = JsonStore.class)))
    @GetMapping("/{name}")
    public ResponseEntity<?> getByName(@PathVariable @NotBlank @Size(max = 191) String name) {
        userService.loggedInUser();
        return jsonStoreService.findByName(name)
                .<ResponseEntity<?>>map(Response::ok)
                .orElse(Response.notFound("Setting not found"));
    }

    @Operation(summary = "Create a JSON store entry")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = JsonStore.class)))
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody JsonStore jsonStore) {
        userService.loggedInUser();
        JsonStore saved = jsonStoreService.save(jsonStore);
        return Response.ok(saved, "Setting saved successfully");
    }

    @Operation(summary = "Update a JSON store entry by name")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = JsonStore.class)))
    @PutMapping("/{name}")
    public ResponseEntity<?> update(@PathVariable @NotBlank @Size(max = 191) String name, @Valid @RequestBody JsonStore jsonStore) {
        userService.loggedInUser();
        jsonStore.setName(name);
        JsonStore updated = jsonStoreService.save(jsonStore);
        return Response.ok(updated, "Setting updated successfully");
    }

    @Operation(summary = "Delete a JSON store entry by name")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "string")))
    @DeleteMapping("/{name}")
    public ResponseEntity<?> delete(@PathVariable @NotBlank @Size(max = 191) String name) {
        userService.loggedInUser();
        jsonStoreService.delete(name);
        return Response.ok("Setting deleted successfully");
    }
}
