package com.trako.controllers;

import com.trako.entities.JsonStore;
import com.trako.services.JsonStoreService;
import com.trako.services.UserService;
import com.trako.util.Response;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/json-store")
public class JsonStoreController {

    @Autowired
    private JsonStoreService jsonStoreService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        userService.loggedInUser();
        List<JsonStore> items = jsonStoreService.findAll();
        return Response.ok(items);
    }

    @GetMapping("/{name}")
    public ResponseEntity<?> getByName(@PathVariable String name) {
        userService.loggedInUser();
        return jsonStoreService.findByName(name)
                .map(Response::ok)
                .orElse(Response.notFound("Setting not found"));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody JsonStore jsonStore) {
        userService.loggedInUser();
        JsonStore saved = jsonStoreService.save(jsonStore);
        return Response.ok(saved, "Setting saved successfully");
    }

    @PutMapping("/{name}")
    public ResponseEntity<?> update(@PathVariable String name, @Valid @RequestBody JsonStore jsonStore) {
        userService.loggedInUser();
        jsonStore.setName(name);
        JsonStore updated = jsonStoreService.save(jsonStore);
        return Response.ok(updated, "Setting updated successfully");
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<?> delete(@PathVariable String name) {
        userService.loggedInUser();
        jsonStoreService.delete(name);
        return Response.ok("Setting deleted successfully");
    }
}
