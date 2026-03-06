package com.trako.services;

import com.trako.entities.User;
import com.trako.entities.UserCurrency;
import com.trako.exceptions.NotFoundException;
import com.trako.models.external.ExchangeRateApiResponse;
import com.trako.repositories.UserCurrencyRepository;
import com.trako.repositories.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CurrencyService {

    @Autowired
    private UsersRepository userRepository;

    @Autowired
    private UserCurrencyRepository userCurrencyRepository;

    @Autowired
    private ExchangeRateService exchangeRateService;

    public List<UserCurrency> getAll(String userId) {
        return userCurrencyRepository.findByUserId(userId);
    }

    public UserCurrency save(User user, String currencyCode, Double exchangeRate) {
        UserCurrency existing = userCurrencyRepository.findByUserIdAndCurrencyCode(user.getId(), currencyCode);
        if (existing != null) {
            existing.setExchangeRate(exchangeRate);
            return userCurrencyRepository.save(existing);
        } else {
            UserCurrency uc = new UserCurrency();
            uc.setUser(user);
            uc.setCurrencyCode(currencyCode);
            uc.setExchangeRate(exchangeRate);
            return userCurrencyRepository.save(uc);
        }
    }

    public UserCurrency save(String userId, String currencyCode, Double exchangeRate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return save(user, currencyCode, exchangeRate);
    }

    public void delete(String userId, String currencyCode) {
        UserCurrency existing = userCurrencyRepository.findByUserIdAndCurrencyCode(userId, currencyCode);
        if (existing != null) {
            userCurrencyRepository.delete(existing);
        }
    }

    public void deleteAllForUser(String userId) {
        userCurrencyRepository.deleteByUserId(userId);
    }

    /**
     * Saves a UserCurrency for the given user and currency code, automatically
     * resolving the exchange rate against the user's base currency using
     * {@link ExchangeRateService}.
     */
    public UserCurrency saveWithAutoRate(User user, String currencyCode) {
        String baseCurrency = user.getBaseCurrency();
        if (baseCurrency == null || baseCurrency.isBlank()) {
            throw new IllegalArgumentException("User does not have a base currency configured");
        }

        String targetCurrency = currencyCode.toUpperCase();
        if (targetCurrency.equalsIgnoreCase(baseCurrency)) {
            return save(user, targetCurrency, 1.0);
        }

        ExchangeRateApiResponse data = exchangeRateService.getRates(baseCurrency);
        Map<String, Double> rates = data.getRates();

        Double value = rates.get(targetCurrency);
        if (value == null) {
            throw new IllegalArgumentException("Currency not supported by rate provider: " + targetCurrency);
        }

        return save(user, targetCurrency, value);
    }

    public UserCurrency saveWithAutoRate(String userId, String currencyCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return saveWithAutoRate(user, currencyCode);
    }

    /**
     * Resolves the exchange rate for a given currency and user.
     * 
     * @param userId the user ID
     * @param currency the currency code to resolve
     * @param providedRate optional explicitly provided rate (returns this if not null)
     * @return the exchange rate
     * @throws IllegalArgumentException if currency is not configured for user
     */
    public Double resolveExchangeRate(String userId, String currency, Double providedRate) {
        if (providedRate != null) {
            return providedRate;
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
            
        if (currency.equals(user.getBaseCurrency())) {
            return 1.0;
        }
        
        UserCurrency uc = userCurrencyRepository.findByUserIdAndCurrencyCode(userId, currency);
        if (uc == null) {
            throw new IllegalArgumentException("Currency not configured: " + currency);
        }
        return uc.getExchangeRate();
    }
}
