package com.trako.services;

import com.trako.entities.Account;
import com.trako.repositories.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
    }

    public List<Account> findByUserId(String userId) {
        return accountRepository.findByUserIdOrderByNameAsc(userId);
    }

    public Account save(Account account) {
        if (account.getName() != null) {
            account.setName(account.getName().trim());
        }
        if (account.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (account.getName() == null || account.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        boolean duplicate;
        if (account.getId() == null) {
            duplicate = accountRepository.existsByUserIdAndNameIgnoreCase(account.getUserId(), account.getName());
        } else {
            duplicate = accountRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(account.getUserId(), account.getName(), account.getId());
        }
        if (duplicate) {
            throw new IllegalArgumentException("Account name already exists");
        }
        return accountRepository.save(account);
    }

    public void delete(Long id) {
        accountRepository.deleteById(id);
    }
}
