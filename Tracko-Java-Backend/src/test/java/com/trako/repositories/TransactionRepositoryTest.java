package com.trako.repositories;

import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class TransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    private User testUser;
    private Account testAccount;
    private Category testCategory;

    @BeforeEach
    public void setup() {
        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        entityManager.persist(testUser);

        testAccount = new Account();
        testAccount.setName("Savings");
        testAccount.setUserId(testUser.getId());
        entityManager.persist(testAccount);

        testCategory = new Category();
        testCategory.setName("Food");
        testCategory.setUserId(testUser.getId());
        entityManager.persist(testCategory);

        entityManager.flush();
    }

    @Test
    public void testSaveTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("Lunch");
        transaction.setComments("Pizza");
        transaction.setDate(new Date());
        transaction.setAmount(25.50);
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());

        Transaction saved = transactionRepository.save(transaction);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Lunch");
        assertThat(saved.getAmount()).isEqualTo(25.50);
    }

    @Test
    public void testFindByUserId() {
        Transaction transaction1 = new Transaction();
        transaction1.setTransactionType(1);
        transaction1.setName("Lunch");
        transaction1.setDate(new Date());
        transaction1.setAmount(25.50);
        transaction1.setAccountId(testAccount.getId());
        transaction1.setCategoryId(testCategory.getId());
        entityManager.persist(transaction1);

        Transaction transaction2 = new Transaction();
        transaction2.setTransactionType(1);
        transaction2.setName("Dinner");
        transaction2.setDate(new Date());
        transaction2.setAmount(35.00);
        transaction2.setAccountId(testAccount.getId());
        transaction2.setCategoryId(testCategory.getId());
        entityManager.persist(transaction2);

        entityManager.flush();

        List<Transaction> transactions = transactionRepository.findByUserId(testUser.getId());

        assertThat(transactions).hasSize(2);
        assertThat(transactions).extracting(Transaction::getName).containsExactlyInAnyOrder("Lunch", "Dinner");
    }

    @Test
    public void testFindByUserIdAndDateBetween() {
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        
        cal.add(Calendar.DAY_OF_MONTH, -5);
        Date fiveDaysAgo = cal.getTime();
        
        cal.add(Calendar.DAY_OF_MONTH, -10);
        Date fifteenDaysAgo = cal.getTime();

        Transaction transaction1 = new Transaction();
        transaction1.setTransactionType(1);
        transaction1.setName("Recent");
        transaction1.setDate(today);
        transaction1.setAmount(25.50);
        transaction1.setAccountId(testAccount.getId());
        transaction1.setCategoryId(testCategory.getId());
        entityManager.persist(transaction1);

        Transaction transaction2 = new Transaction();
        transaction2.setTransactionType(1);
        transaction2.setName("Old");
        transaction2.setDate(fifteenDaysAgo);
        transaction2.setAmount(35.00);
        transaction2.setAccountId(testAccount.getId());
        transaction2.setCategoryId(testCategory.getId());
        entityManager.persist(transaction2);

        entityManager.flush();

        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetween(
            testUser.getId(), fiveDaysAgo, today);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getName()).isEqualTo("Recent");
    }

    @Test
    public void testFindByAccountId() {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("Test Transaction");
        transaction.setDate(new Date());
        transaction.setAmount(100.00);
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        entityManager.persist(transaction);
        entityManager.flush();

        List<Transaction> transactions = transactionRepository.findByAccountId(testAccount.getId());

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getName()).isEqualTo("Test Transaction");
    }

    @Test
    public void testFindByCategoryId() {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("Food Purchase");
        transaction.setDate(new Date());
        transaction.setAmount(50.00);
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        entityManager.persist(transaction);
        entityManager.flush();

        List<Transaction> transactions = transactionRepository.findByCategoryId(testCategory.getId());

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getName()).isEqualTo("Food Purchase");
    }
}
