package com.trako.controllers;

import com.trako.entities.Contact;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.models.request.ContactSaveRequest;
import com.trako.services.ContactService;
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

@Tag(name = "Contacts", description = "Manage contacts for expense splitting")
@RestController
@RequestMapping("/api/contacts")
@Validated
public class ContactController {

    @Autowired
    private ContactService contactService;

    @Autowired
    private UserService userService;

    @Operation(summary = "List all contacts for the current user")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Contact.class))))
    @GetMapping
    public ResponseEntity<?> listMine() {
        try {
            String currentUserId = userService.loggedInUser().getId();
            List<Contact> contacts = contactService.findByUserId(currentUserId);
            return Response.ok(contacts);
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @Operation(summary = "Get a contact by ID")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Contact.class)))
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable @Positive Long id) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            var opt = contactService.findByIdAndUserId(id, currentUserId);
            if (opt.isPresent()) {
                return Response.ok(opt.get());
            }
            return Response.notFound("Contact not found");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @Operation(summary = "Create a new contact")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Contact.class)))
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ContactSaveRequest request) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            Contact c = new Contact();
            c.setUserId(currentUserId);
            c.setName(request.getName());
            c.setPhoneNo(request.getPhoneNo());
            c.setEmail(request.getEmail());
            Contact saved = contactService.save(c);
            return Response.ok(saved, "Contact created successfully");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @Operation(summary = "Update a contact")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Contact.class)))
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable @Positive Long id, @Valid @RequestBody ContactSaveRequest request) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            // Ensure ownership
            var opt = contactService.findByIdAndUserId(id, currentUserId);
            if (opt.isPresent()) {
                Contact existing = opt.get();
                existing.setName(request.getName());
                existing.setPhoneNo(request.getPhoneNo());
                existing.setEmail(request.getEmail());
                Contact saved = contactService.save(existing);
                return Response.ok(saved, "Contact updated successfully");
            }
            return Response.notFound("Contact not found");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @Operation(summary = "Delete a contact")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "string")))
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable @Positive Long id) {
        try {
            String currentUserId = userService.loggedInUser().getId();
            var opt = contactService.findByIdAndUserId(id, currentUserId);
            if (opt.isPresent()) {
                Contact existing = opt.get();
                contactService.delete(existing.getId());
                return Response.ok("Contact deleted successfully");
            }
            return Response.notFound("Contact not found");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }
}
