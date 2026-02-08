package com.trako.controllers;

import com.trako.entities.User;
import com.trako.entities.UserCurrency;
import com.trako.models.request.UserCurrencyRequest;
import com.trako.repositories.UserCurrencyRepository;
import com.trako.services.UserService;
import com.trako.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user-currencies")
public class UserCurrencyController {

    @Autowired
    UserService userService;

    @Autowired
    UserCurrencyRepository userCurrencyRepository;

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            User user = userService.loggedInUser();
            // Since we changed to EAGER fetch in User, we can just return the list from user
            // But to maintain backward compatibility/safety, we can still use repository or user.getSecondaryCurrencies()
            // Using repository ensures we get what's in DB
            List<UserCurrency> currencies = userCurrencyRepository.findByUserId(user.getId());
            return Response.ok(currencies);
        } catch (Exception e) {
            return Response.unauthorized();
        }
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody UserCurrencyRequest request) {
        try {
            User user = userService.loggedInUser();
            if (user.getSecondaryCurrencies() == null) {
                user.setSecondaryCurrencies(new ArrayList<UserCurrency>());
            }
            List<UserCurrency> currencies = user.getSecondaryCurrencies();

            Optional<UserCurrency> existingOpt = currencies.stream()
                    .filter(uc -> uc.getCurrencyCode().equals(request.getCurrencyCode()))
                    .findFirst();

            if (existingOpt.isPresent()) {
                existingOpt.get().setExchangeRate(request.getExchangeRate());
            } else {
                UserCurrency uc = new UserCurrency();
                uc.setUser(user);
                uc.setCurrencyCode(request.getCurrencyCode());
                uc.setExchangeRate(request.getExchangeRate());
                currencies.add(uc);
            }

            userService.saveUser(user);
            return Response.ok("Saved", "Saved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return Response.unauthorized();
        }
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<?> delete(@PathVariable String code) {
        try {
            User user = userService.loggedInUser();
            if (user.getSecondaryCurrencies() != null) {
                boolean removed = user.getSecondaryCurrencies().removeIf(uc -> uc.getCurrencyCode().equals(code));
                if (removed) {
                    userService.saveUser(user);
                }
            }
            return Response.ok("Deleted", "Deleted successfully");
        } catch (Exception e) {
            return Response.unauthorized();
        }
    }
}
