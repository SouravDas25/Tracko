package com.trako.controllers;

import com.trako.entities.User;
import com.trako.entities.UserCurrency;
import com.trako.models.request.UserCurrencyRequest;
import com.trako.services.CurrencyService;
import com.trako.services.UserService;
import com.trako.util.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User Currencies", description = "Manage currencies configured for the current user")
@RestController
@RequestMapping("/api/user-currencies")
@Validated
public class UserCurrencyController {

    @Autowired
    UserService userService;

    @Autowired
    CurrencyService currencyService;

    @Operation(summary = "List currencies configured for the current user")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserCurrency.class))))
    @GetMapping
    public ResponseEntity<?> getAll() {
        User user = userService.loggedInUser();
        List<UserCurrency> currencies = currencyService.getAll(user.getId());
        return Response.ok(currencies);
    }

    @Operation(summary = "Add or update a currency with a manual exchange rate")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "string")))
    @PostMapping
    public ResponseEntity<?> save(@Valid @RequestBody UserCurrencyRequest request) {
        User user = userService.loggedInUser();
        currencyService.save(user, request.getCurrencyCode(), request.getExchangeRate());
        return Response.ok("Saved", "Saved successfully");
    }

    /**
     * Saves a currency for the logged-in user using an automatically
     * fetched exchange rate against the user's base currency.
     */
    @Operation(summary = "Add a currency with an automatically fetched exchange rate")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "string")))
    @PostMapping("/auto")
    public ResponseEntity<?> saveAuto(@RequestParam @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter currency code") String currencyCode) {
        User user = userService.loggedInUser();
        currencyService.saveWithAutoRate(user, currencyCode);
        return Response.ok("Saved", "Saved successfully");
    }

    @Operation(summary = "Remove a currency from the current user")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(type = "string")))
    @DeleteMapping("/{code}")
    public ResponseEntity<?> delete(@PathVariable String code) {
        User user = userService.loggedInUser();
        currencyService.delete(user.getId(), code);
        return Response.ok("Deleted", "Deleted successfully");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {
        return Response.badRequest("Invalid request");
    }
}
