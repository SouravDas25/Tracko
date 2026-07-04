package com.trako.integration.transaction;

import com.trako.entities.Account;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.enums.TransactionDbType;
import com.trako.integration.BaseIntegrationTest;
import com.trako.repositories.TransactionRepository;
import com.trako.services.transactions.TransferService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

/**
 * Atomicity coverage for {@link TransferService#createTransfer}.
 *
 * <p>The other transfer ITs are {@code @Transactional}, so the whole test + service run in one
 * transaction that always rolls back — a genuine mid-operation failure is never committed or
 * observed. This class is deliberately <b>not</b> {@code @Transactional}: the service runs in its
 * own transaction, we force the credit leg to fail <i>after</i> the debit leg has been flushed, and
 * then observe (from a fresh transaction) that the debit was rolled back too.
 *
 * <p>Because the H2 test database is shared for the whole JVM run
 * ({@code DB_CLOSE_DELAY=-1}), committed setup rows are cleaned up in {@link #cleanUp()} so this
 * class cannot pollute the global {@code findAll()} assertions in the other transfer tests.
 */
@SpringBootTest
@ActiveProfiles("test")
public class TransferAtomicityIT extends BaseIntegrationTest {

    @Autowired
    private TransferService transferService;

    // Replaces the TransactionRepository bean everywhere with a spy so we can fail the credit save.
    @SpyBean
    private TransactionRepository transactionRepository;

    @Autowired
    private PlatformTransactionManager txManager;

    private User user;
    private Account source;
    private Account dest;

    @BeforeEach
    public void setUp() {
        user = createUniqueUser("Atomicity User");

        source = new Account();
        source.setName("Atomic Source");
        source.setUserId(user.getId());
        source = accountRepository.save(source);

        dest = new Account();
        dest.setName("Atomic Dest");
        dest.setUserId(user.getId());
        dest = accountRepository.save(dest);
    }

    @AfterEach
    public void cleanUp() {
        // Drop the stubbing before cleanup so the delete queries hit the real repository.
        Mockito.reset(transactionRepository);
        // Bulk @Modifying deletes need an active transaction; the test itself is not @Transactional.
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            transactionRepository.deleteByAccountIdIn(List.of(source.getId(), dest.getId()));
            categoryRepository.deleteByUserId(user.getId());
            accountRepository.deleteByUserId(user.getId());
            usersRepository.deleteById(user.getId());
        });
    }

    @Test
    public void createTransfer_whenCreditSaveFails_rollsBackDebit() {
        // The debit leg is saved first (real call); force the credit leg's saveAndFlush to blow up.
        doThrow(new RuntimeException("simulated credit failure"))
                .when(transactionRepository)
                .saveAndFlush(argThat(t -> t != null && t.getTransactionType() == TransactionDbType.CREDIT));

        assertThrows(RuntimeException.class, () -> transferService.createTransfer(
                user.getId(), source.getId(), dest.getId(),
                new Date(), 100.0, "INR", 1.0, "Atomic Transfer", "test"));

        // Fresh query after the service transaction rolled back: neither leg may survive.
        Mockito.reset(transactionRepository); // so the read below is a real call
        List<Transaction> remaining = transactionRepository.findByAccountIdIn(List.of(source.getId(), dest.getId()));
        assertTrue(remaining.isEmpty(),
                "Debit leg must be rolled back when the credit leg fails (transfer must be atomic)");
    }
}
