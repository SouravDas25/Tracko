package com.trako.controllers;

import com.trako.entities.User;
import com.trako.entities.UserCurrency;
import com.trako.models.request.UserCurrencyRequest;
import com.trako.services.CurrencyService;
import com.trako.services.UserService;
import com.trako.util.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-currencies")
@Validated
public class UserCurrencyController {

    @Autowired
    UserService userService;

    @Autowired
    CurrencyService currencyService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        User user = userService.loggedInUser();
        List<UserCurrency> currencies = currencyService.getAll(user.getId());
        return Response.ok(currencies);
    }

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
    @PostMapping("/auto")
    public ResponseEntity<?> saveAuto(@RequestParam @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter currency code") String currencyCode) {
        User user = userService.loggedInUser();
        currencyService.saveWithAutoRate(user, currencyCode);
        return Response.ok("Saved", "Saved successfully");
    }

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
