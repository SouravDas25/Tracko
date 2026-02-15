package com.trako.repositories;

import com.trako.entities.UserCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCurrencyRepository extends JpaRepository<UserCurrency, Long> {
    List<UserCurrency> findByUserId(String userId);
    UserCurrency findByUserIdAndCurrencyCode(String userId, String currencyCode);
}
