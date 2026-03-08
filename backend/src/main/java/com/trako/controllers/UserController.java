package com.trako.controllers;

import com.trako.entities.User;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.models.request.UserProfileUpdateRequest;
import com.trako.models.request.UserSaveRequest;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;


@Tag(name = "Users", description = "User management and profile")
@RestController
@RequestMapping("/api/user")
@Validated
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;

    @Operation(summary = "List users (admin) or get a user by ID")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = User.class))))
    @GetMapping(value = {"", "/{id}"})
    ResponseEntity<?> show(@PathVariable(required = false) @Size(max = 36) String id) {
        User current = userService.loggedInUser();

        if (id == null || id.isBlank()) {
            if (!current.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(com.trako.models.responses.ApiResponse.make(null, "Access denied"));
            }
            return Response.ok(userService.findUser(null));
        }
        if (!current.isAdmin() && !current.getId().equals(id)) {
            return Response.unauthorized();
        }

        if (current.isAdmin()) {
            User target = userService.findById(id);
            if (target == null) {
                return Response.notFound("User not found");
            }
            return Response.ok(Collections.singletonList(target));
        }

        return Response.ok(Collections.singletonList(current));
    }

    @Operation(summary = "Get the current user's profile")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = User.class)))
    @GetMapping(value = "/me")
    ResponseEntity<?> me() {
        User user = userService.loggedInUser();
        return Response.ok(user);
    }

    @Operation(summary = "Look up a user by phone number")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = User.class))))
    @GetMapping(value = "/byPhoneNo")
    ResponseEntity<?> showByPhone(@RequestParam("phone_no") @NotBlank @Size(max = 32) String phoneNo) {
        // prevent user enumeration; only allow lookup when authenticated
        userService.loggedInUser();
        User byPhoneNo = userService.findByPhoneNo(phoneNo);
        if (byPhoneNo == null) {
            return Response.ok("Resource Empty");
        }
        List<User> users = Collections.singletonList(byPhoneNo);
        return Response.ok(users);
    }

    @Operation(summary = "Create a new user (admin only)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "string", description = "New user ID")))
    @PostMapping(value = "/create")
    ResponseEntity<?> create(@Valid @RequestBody UserSaveRequest userSaveRequest) {
        try {
            User current = userService.loggedInUser();

            // Only admins can create users
            if (!current.isAdmin()) {
                log.warn("Non-admin user {} attempted to create a user", current.getId());
                return Response.unauthorized();
            }

            String id = userService.save(userSaveRequest);
            if (id == null) {
                return Response.badRequest("Invalid user request");
            }

            log.info("Admin {} created user: {}", current.getId(), id);
            return Response.ok(id, "User Created Successfully.");
        } catch (UserNotLoggedInException e) {
            return Response.unauthorized();
        }
    }

    @Operation(summary = "Update the current user's profile")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "string", description = "User ID")))
    @PostMapping(value = "/me")
    ResponseEntity<?> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        User current = userService.loggedInUser();

        // Only allow updating own profile fields
        if (request.getName() != null) {
            current.setName(request.getName());
        }
        if (request.getEmail() != null) {
            current.setEmail(request.getEmail());
        }
        if (request.getProfilePic() != null) {
            current.setProfilePic(request.getProfilePic());
        }
        if (request.getBaseCurrency() != null) {
            current.setBaseCurrency(request.getBaseCurrency());
        }

        User saved = userService.saveUser(current);
        log.info("User {} updated their profile", saved.getId());
        return Response.ok(saved.getId(), "Profile Updated Successfully.");
    }

    @Operation(summary = "Reset all user data (accounts, categories, transactions)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "string")))
    @DeleteMapping(value = "/data")
    ResponseEntity<?> resetData() {
        User current = userService.loggedInUser();
        userService.resetUserData(current.getId());
        return Response.ok("User data reset successfully.");
    }

    @Operation(summary = "Reset only transactions for the current user")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "string")))
    @DeleteMapping(value = "/transactions")
    ResponseEntity<?> resetTransactions() {
        User current = userService.loggedInUser();
        userService.resetUserTransactions(current.getId());
        return Response.ok("User transactions reset successfully.");
    }

}
