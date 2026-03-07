package com.trako.repositories;

import com.trako.entities.UserCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCurrencyRepository extends JpaRepository<UserCurrency, Long> {
    List<UserCurrency> findByUserId(String userId);

    UserCurrency findByUserIdAndCurrencyCode(String userId, String currencyCode);

    @Modifying
    @Query("DELETE FROM UserCurrency u WHERE u.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
