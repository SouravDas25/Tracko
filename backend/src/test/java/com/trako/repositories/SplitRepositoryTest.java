package com.trako.repositories;

import com.trako.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class SplitRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SplitRepository splitRepository;

    private User testUser;
    private Transaction testTransaction;

    @BeforeEach
    public void setup() {
        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        entityManager.persist(testUser);

        Account testAccount = new Account();
        testAccount.setName("Savings");
        testAccount.setUserId(testUser.getId());
        entityManager.persist(testAccount);

        Category testCategory = new Category();
        testCategory.setName("Food");
        testCategory.setUserId(testUser.getId());
        entityManager.persist(testCategory);

        testTransaction = new Transaction();
        testTransaction.setTransactionType(1);
        testTransaction.setName("Dinner");
        testTransaction.setDate(new Date());
        testTransaction.setAmount(100.00);
        testTransaction.setAccountId(testAccount.getId());
        testTransaction.setCategoryId(testCategory.getId());
        entityManager.persist(testTransaction);

        entityManager.flush();
    }

    @Test
    public void testSaveSplit() {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(50.00);
        split.setIsSettled(0);

        Split saved = splitRepository.save(split);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAmount()).isEqualTo(50.00);
        assertThat(saved.getIsSettled()).isEqualTo(0);
    }

    @Test
    public void testFindByTransactionId() {
        Split split1 = new Split();
        split1.setTransactionId(testTransaction.getId());
        split1.setUserId(testUser.getId());
        split1.setAmount(50.00);
        entityManager.persist(split1);

        Split split2 = new Split();
        split2.setTransactionId(testTransaction.getId());
        split2.setUserId(testUser.getId());
        split2.setAmount(50.00);
        entityManager.persist(split2);

        entityManager.flush();

        List<Split> splits = splitRepository.findByTransactionId(testTransaction.getId());

        assertThat(splits).hasSize(2);
    }

    @Test
    public void testFindByUserId() {
        Split split = new Split();
        split.setTransactionId(testTransaction.getId());
        split.setUserId(testUser.getId());
        split.setAmount(100.00);
        entityManager.persist(split);
        entityManager.flush();

        List<Split> splits = splitRepository.findByUserId(testUser.getId());

        assertThat(splits).hasSize(1);
        assertThat(splits.get(0).getAmount()).isEqualTo(100.00);
    }

    @Test
    public void testFindByUserIdAndIsSettled() {
        Split split1 = new Split();
        split1.setTransactionId(testTransaction.getId());
        split1.setUserId(testUser.getId());
        split1.setAmount(50.00);
        split1.setIsSettled(0);
        entityManager.persist(split1);

        Split split2 = new Split();
        split2.setTransactionId(testTransaction.getId());
        split2.setUserId(testUser.getId());
        split2.setAmount(50.00);
        split2.setIsSettled(1);
        entityManager.persist(split2);

        entityManager.flush();

        List<Split> unsettled = splitRepository.findByUserIdAndIsSettled(testUser.getId(), 0);

        assertThat(unsettled).hasSize(1);
        assertThat(unsettled.get(0).getIsSettled()).isEqualTo(0);
    }
}
