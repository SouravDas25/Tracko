package com.trako.integration.recurring;

import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.RecurringTransaction;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.enums.CategoryType;
import com.trako.enums.Frequency;
import com.trako.enums.TransactionType;
import com.trako.integration.BaseIntegrationTest;
import com.trako.repositories.RecurringTransactionRepository;
import com.trako.services.RecurringTransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the real scheduler catch-up path:
 * {@link RecurringTransactionService#processDueTransactions()} →
 * {@code self.processSingleTransaction(...)} which is {@code @Transactional(REQUIRES_NEW)}.
 *
 * <p>{@link RecurringTransactionIntegrationTest} deliberately drives only {@code create()} (a direct,
 * same-transaction call) because a {@code REQUIRES_NEW} transaction cannot see a {@code @Transactional}
 * test's uncommitted rows. This class is therefore <b>not</b> {@code @Transactional}: it commits the
 * recurring definition, invokes the scheduler entry point (which runs each item in its own committed
 * transaction via the Spring proxy), and asserts the backfilled transaction was committed.
 *
 * <p>The H2 test DB is shared for the whole JVM run ({@code DB_CLOSE_DELAY=-1}), so all committed rows
 * are removed in {@link #cleanUp()} to avoid polluting other tests. Assertions are scoped to this
 * test's own account so a stray due row elsewhere cannot affect the result.
 */
@SpringBootTest
@ActiveProfiles("test")
public class RecurringSchedulerIT extends BaseIntegrationTest {

    @Autowired
    private RecurringTransactionService recurringTransactionService;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private PlatformTransactionManager txManager;

    private User user;
    private Account account;
    private Category category;
    private RecurringTransaction rt;

    @BeforeEach
    public void setUp() {
        user = createUniqueUser("Scheduler User");

        account = new Account();
        account.setName("Sched Account");
        account.setUserId(user.getId());
        account.setCurrency("INR");
        account = accountRepository.save(account);

        category = new Category();
        category.setName("Sched Category");
        category.setUserId(user.getId());
        category.setCategoryType(CategoryType.EXPENSE);
        category = categoryRepository.save(category);

        // A committed, active, past-due monthly recurring definition (nextRunDate = yesterday).
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = cal.getTime();

        RecurringTransaction r = new RecurringTransaction();
        r.setUserId(user.getId());
        r.setName("Sched RT");
        r.setOriginalAmount(500.0);
        r.setOriginalCurrency("INR");
        r.setExchangeRate(1.0);
        r.setAccountId(account.getId());
        r.setCategoryId(category.getId());
        r.setTransactionType(TransactionType.DEBIT);
        r.setFrequency(Frequency.MONTHLY);
        r.setStartDate(yesterday);
        r.setNextRunDate(yesterday);
        r.setIsActive(true);
        rt = recurringTransactionRepository.save(r);
    }

    @AfterEach
    public void cleanUp() {
        // Delete each level in its own committed transaction, in FK-safe order. A single wrapping
        // transaction is not enough: a derived delete (transactions) defers its removes to commit
        // while a bulk @Modifying delete (categories) executes immediately, which would delete a
        // category while a transaction still references it. Committing per level avoids that.
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.executeWithoutResult(s -> transactionRepository.deleteByAccountIdIn(List.of(account.getId())));
        tt.executeWithoutResult(s -> recurringTransactionRepository.deleteAll(
                recurringTransactionRepository.findByUserId(user.getId())));
        tt.executeWithoutResult(s -> categoryRepository.deleteByUserId(user.getId()));
        tt.executeWithoutResult(s -> accountRepository.deleteByUserId(user.getId()));
        tt.executeWithoutResult(s -> usersRepository.deleteById(user.getId()));
    }

    @Test
    public void processDueTransactions_commitsBackfillViaRequiresNew() {
        assertTrue(transactionRepository.findByAccountId(account.getId()).isEmpty(),
                "precondition: no transactions before the scheduler runs");

        // Real scheduler entry point: self-proxied REQUIRES_NEW processing, exactly as the cron job runs it.
        recurringTransactionService.processDueTransactions();

        List<Transaction> created = transactionRepository.findByAccountId(account.getId());
        assertEquals(1, created.size(), "scheduler must commit exactly one backfilled transaction");
        assertEquals("Sched RT", created.get(0).getName());

        // The definition must have been advanced and persisted in its own committed transaction.
        RecurringTransaction after = recurringTransactionRepository.findById(rt.getId()).orElseThrow();
        assertNotNull(after.getLastRunDate(), "lastRunDate must be persisted by the REQUIRES_NEW transaction");
        assertTrue(after.getNextRunDate().after(new Date()), "nextRunDate must advance into the future");
    }
}
