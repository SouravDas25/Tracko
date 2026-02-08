package com.trako.controllers;

import com.trako.entities.JsonStore;
import com.trako.services.JsonStoreService;
import com.trako.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/json-store")
public class JsonStoreController {

    @Autowired
    private JsonStoreService jsonStoreService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<JsonStore> items = jsonStoreService.findAll();
        return Response.ok(items);
    }

    @GetMapping("/{name}")
    public ResponseEntity<?> getByName(@PathVariable String name) {
        return jsonStoreService.findByName(name)
                .map(Response::ok)
                .orElse(Response.notFound("Setting not found"));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody JsonStore jsonStore) {
        JsonStore saved = jsonStoreService.save(jsonStore);
        return Response.ok(saved, "Setting saved successfully");
    }

    @PutMapping("/{name}")
    public ResponseEntity<?> update(@PathVariable String name, @Valid @RequestBody JsonStore jsonStore) {
        jsonStore.setName(name);
        JsonStore updated = jsonStoreService.save(jsonStore);
        return Response.ok(updated, "Setting updated successfully");
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<?> delete(@PathVariable String name) {
        jsonStoreService.delete(name);
        return Response.ok("Setting deleted successfully");
    }
}
