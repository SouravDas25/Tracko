package com.trako.repositories;

import com.trako.entities.Account;
import com.trako.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    private User testUser;

    @BeforeEach
    public void setup() {
        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    public void testSaveAccount() {
        Account account = new Account();
        account.setName("Savings");
        account.setUserId(testUser.getId());

        Account saved = accountRepository.save(account);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Savings");
        assertThat(saved.getUserId()).isEqualTo(testUser.getId());
    }

    @Test
    public void testFindByUserId() {
        Account account1 = new Account();
        account1.setName("Savings");
        account1.setUserId(testUser.getId());
        entityManager.persist(account1);

        Account account2 = new Account();
        account2.setName("Cash");
        account2.setUserId(testUser.getId());
        entityManager.persist(account2);

        entityManager.flush();

        List<Account> accounts = accountRepository.findByUserId(testUser.getId());

        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(Account::getName).containsExactlyInAnyOrder("Savings", "Cash");
    }

    @Test
    public void testFindById() {
        Account account = new Account();
        account.setName("Checking");
        account.setUserId(testUser.getId());
        Account saved = entityManager.persist(account);
        entityManager.flush();

        Account found = accountRepository.findById(saved.getId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Checking");
    }

    @Test
    public void testDeleteAccount() {
        Account account = new Account();
        account.setName("Temporary");
        account.setUserId(testUser.getId());
        Account saved = entityManager.persist(account);
        entityManager.flush();

        accountRepository.deleteById(saved.getId());

        Account found = accountRepository.findById(saved.getId()).orElse(null);
        assertThat(found).isNull();
    }
}
