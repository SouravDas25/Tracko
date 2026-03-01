package com.trako.services;

import com.trako.entities.Account;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.RecurringTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;

    @BeforeEach
    public void setup() {
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setName("Savings");
        testAccount.setUserId("user123");
    }

    @Test
    public void testFindAll() {
        when(accountRepository.findAll()).thenReturn(Arrays.asList(testAccount));

        List<Account> accounts = accountService.findAll();

        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getName()).isEqualTo("Savings");
        verify(accountRepository, times(1)).findAll();
    }

    @Test
    public void testFindById() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        Optional<Account> found = accountService.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Savings");
        verify(accountRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindByUserId() {
        when(accountRepository.findByUserIdOrderByNameAsc("user123")).thenReturn(Arrays.asList(testAccount));

        List<Account> accounts = accountService.findByUserId("user123");

        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getUserId()).isEqualTo("user123");
        verify(accountRepository, times(1)).findByUserIdOrderByNameAsc("user123");
    }

    @Test
    public void testSave() {
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        Account saved = accountService.save(testAccount);

        assertThat(saved).isNotNull();
        assertThat(saved.getName()).isEqualTo("Savings");
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    public void testDelete() {
        when(transactionRepository.existsByAccountId(1L)).thenReturn(false);
        when(recurringTransactionRepository.existsByAccountId(1L)).thenReturn(false);
        when(recurringTransactionRepository.existsByToAccountId(1L)).thenReturn(false);
        doNothing().when(accountRepository).deleteById(1L);

        accountService.delete(1L);

        verify(accountRepository, times(1)).deleteById(1L);
    }
}
