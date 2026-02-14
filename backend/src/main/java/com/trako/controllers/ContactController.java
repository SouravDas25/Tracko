package com.trako.controllers;

import com.trako.entities.Contact;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.models.request.ContactSaveRequest;
import com.trako.services.ContactService;
import com.trako.services.UserService;
import com.trako.util.Response;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @Autowired
    private UserService userService;

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

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
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

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ContactSaveRequest request) {
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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
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
