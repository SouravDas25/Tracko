package com.trako.repositories;

import com.trako.entities.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(String userId);

    List<Account> findByUserIdOrderByNameAsc(String userId);

    boolean existsByUserIdAndNameIgnoreCase(String userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(String userId, String name, Long id);

    @Modifying
    @Query("DELETE FROM Account a WHERE a.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
