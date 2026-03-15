package com.trako.integration.recurring;

import com.trako.entities.*;
import com.trako.enums.CategoryType;
import com.trako.enums.Frequency;
import com.trako.enums.TransactionDbType;
import com.trako.enums.TransactionType;
import com.trako.integration.BaseIntegrationTest;
import com.trako.repositories.RecurringTransactionRepository;
import com.trako.services.RecurringTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RecurringTransactionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private RecurringTransactionService recurringTransactionService;

    private User testUser;
    private Account testAccount;
    private Category testCategory;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("Test User");
        bearerToken = generateBearerToken(testUser);

        testAccount = new Account();
        testAccount.setName("Test Account");
        testAccount.setUserId(testUser.getId());
        testAccount.setCurrency("INR");
        testAccount = accountRepository.save(testAccount);

        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setUserId(testUser.getId());
        testCategory.setCategoryType(CategoryType.EXPENSE);
        testCategory = categoryRepository.save(testCategory);
    }

    // ==================== Helpers ====================

    private RecurringTransaction buildRt(Date startDate) {
        RecurringTransaction rt = new RecurringTransaction();
        rt.setUserId(testUser.getId());
        rt.setName("Test RT");
        rt.setOriginalAmount(500.0);
        rt.setOriginalCurrency("INR");
        rt.setExchangeRate(1.0);
        rt.setAccountId(testAccount.getId());
        rt.setCategoryId(testCategory.getId());
        rt.setTransactionType(TransactionType.DEBIT);
        rt.setFrequency(Frequency.MONTHLY);
        rt.setStartDate(startDate);
        rt.setNextRunDate(startDate);
        rt.setIsActive(true);
        return rt;
    }

    private Date monthsAgo(int months) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -months);
        return cal.getTime();
    }

    private Date monthsFromNow(int months) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, months);
        return cal.getTime();
    }

    // ==================== Create ====================

    @Test
    public void create_withCurrentDate_createsOneTransactionImmediately() throws Exception {
        RecurringTransaction rt = new RecurringTransaction();
        rt.setName("Netflix");
        rt.setOriginalAmount(199.0);
        rt.setOriginalCurrency("INR");
        rt.setExchangeRate(1.0);
        rt.setAccountId(testAccount.getId());
        rt.setCategoryId(testCategory.getId());
        rt.setTransactionType(TransactionType.DEBIT);
        rt.setFrequency(Frequency.MONTHLY);
        Date now = new Date();
        rt.setStartDate(now);
        rt.setNextRunDate(now);

        mockMvc.perform(post("/api/recurring-transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").exists())
                .andExpect(jsonPath("$.result.name").value("Netflix"));

        assertEquals(1, recurringTransactionRepository.count());
        // Immediate backfill: one transaction for the current period
        assertEquals(1, transactionRepository.count());
    }

    @Test
    public void create_withPastStartDate_backfillsMissedTransactions() {
        // Start 3 months ago → should create at least 3 monthly transactions immediately.
        // Exact count is 3 or 4 depending on whether the advanced nextRunDate is still <= now
        // (month boundary arithmetic is not millisecond-precise), so we assert >= 3.
        RecurringTransaction rt = buildRt(monthsAgo(3));

        RecurringTransaction saved = recurringTransactionService.create(testUser.getId(), rt);

        List<Transaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.size() >= 3, "Expected at least 3 backfilled transactions");
        assertTrue(transactions.stream().allMatch(t -> "Test RT".equals(t.getName())));
        assertNotNull(saved.getLastRunDate());
        assertTrue(saved.getNextRunDate().after(new Date()));
    }

    @Test
    public void create_withCurrency_backfillUsesProvidedExchangeRate() {
        RecurringTransaction rt = new RecurringTransaction();
        rt.setName("USD Sub");
        rt.setOriginalCurrency("USD");
        rt.setOriginalAmount(15.0);
        rt.setExchangeRate(80.0);
        rt.setAccountId(testAccount.getId());
        rt.setCategoryId(testCategory.getId());
        rt.setTransactionType(TransactionType.DEBIT);
        rt.setFrequency(Frequency.MONTHLY);
        Date now = new Date();
        rt.setStartDate(now);
        rt.setNextRunDate(now);

        recurringTransactionService.create(testUser.getId(), rt);

        Transaction t = transactionRepository.findAll().get(0);
        assertEquals(15.0, t.getOriginalAmount());
        assertEquals(80.0, t.getExchangeRate());
        // amount = originalAmount * exchangeRate
        assertEquals(1200.0, t.getAmount());
    }

    @Test
    public void create_withEndDateInPast_createsNoTransactionsAndDeactivates() {
        // endDate is in the past — nothing should be created
        RecurringTransaction rt = buildRt(monthsAgo(3));
        rt.setEndDate(monthsAgo(6)); // end date is before startDate

        RecurringTransaction saved = recurringTransactionService.create(testUser.getId(), rt);

        assertEquals(0, transactionRepository.count());
        assertFalse(saved.getIsActive());
    }

    @Test
    public void create_endDateHitDuringCatchUp_stopsEarlyAndDeactivates() {
        // startDate 3 months ago, endDate 2 months ago.
        // End date check is strictly .after(), so the entry ON the endDate is still created.
        // Entries: 3mo ago ✓, 2mo ago ✓, next advance (1mo ago) > endDate → deactivate.
        RecurringTransaction rt = buildRt(monthsAgo(3));
        rt.setEndDate(monthsAgo(2));

        RecurringTransaction saved = recurringTransactionService.create(testUser.getId(), rt);

        assertEquals(2, transactionRepository.count());
        assertFalse(saved.getIsActive());
    }

    // ==================== Ownership checks on create ====================

    @Test
    public void create_unauthorizedAccount_returnsUnauthorized() throws Exception {
        User other = createUniqueUser("Other");
        Account otherAcc = new Account();
        otherAcc.setName("OtherAcc");
        otherAcc.setUserId(other.getId());
        otherAcc = accountRepository.save(otherAcc);

        RecurringTransaction rt = new RecurringTransaction();
        rt.setName("R");
        rt.setOriginalAmount(100.0);
        rt.setOriginalCurrency("INR");
        rt.setExchangeRate(1.0);
        rt.setAccountId(otherAcc.getId()); // belongs to other user
        rt.setCategoryId(testCategory.getId());
        rt.setTransactionType(TransactionType.DEBIT);
        rt.setFrequency(Frequency.MONTHLY);
        Date now = new Date();
        rt.setStartDate(now);
        rt.setNextRunDate(now);

        mockMvc.perform(post("/api/recurring-transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rt)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void create_unauthorizedCategory_returnsUnauthorized() throws Exception {
        User other = createUniqueUser("Other2");
        Category otherCat = new Category();
        otherCat.setName("OtherCat");
        otherCat.setUserId(other.getId());
        otherCat = categoryRepository.save(otherCat);

        RecurringTransaction rt = new RecurringTransaction();
        rt.setName("R");
        rt.setOriginalAmount(100.0);
        rt.setOriginalCurrency("INR");
        rt.setExchangeRate(1.0);
        rt.setAccountId(testAccount.getId());
        rt.setCategoryId(otherCat.getId()); // belongs to other user
        rt.setTransactionType(TransactionType.DEBIT);
        rt.setFrequency(Frequency.MONTHLY);
        Date now = new Date();
        rt.setStartDate(now);
        rt.setNextRunDate(now);

        mockMvc.perform(post("/api/recurring-transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rt)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== Ownership checks on update ====================

    @Test
    public void update_unauthorizedAccount_returnsUnauthorized() throws Exception {
        User other = createUniqueUser("Other3");
        Account otherAcc = new Account();
        otherAcc.setName("OtherAcc");
        otherAcc.setUserId(other.getId());
        otherAcc = accountRepository.save(otherAcc);

        RecurringTransaction rt = buildRt(new Date());
        rt = recurringTransactionRepository.save(rt);

        Map<String, Object> update = new HashMap<>();
        update.put("accountId", otherAcc.getId());

        mockMvc.perform(put("/api/recurring-transactions/" + rt.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void update_crossUserAccess_returnsUnauthorized() throws Exception {
        User other = createUniqueUser("Other4");
        String otherToken = generateBearerToken(other);

        RecurringTransaction rt = buildRt(new Date());
        rt = recurringTransactionRepository.save(rt);

        Map<String, Object> update = new HashMap<>();
        update.put("name", "Hacked");

        mockMvc.perform(put("/api/recurring-transactions/" + rt.getId())
                        .header("Authorization", otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== Delete ====================

    @Test
    public void delete_crossUserAccess_returnsUnauthorized() throws Exception {
        User other = createUniqueUser("Other5");
        String otherToken = generateBearerToken(other);

        RecurringTransaction rt = buildRt(new Date());
        rt = recurringTransactionRepository.save(rt);

        mockMvc.perform(delete("/api/recurring-transactions/" + rt.getId())
                        .header("Authorization", otherToken))
                .andExpect(status().isUnauthorized());
    }

    // ==================== Scheduler catch-up ====================

    // ==================== Scheduler catch-up (exercised via create) ====================
    // processSingleTransaction uses REQUIRES_NEW which cannot see uncommitted test data.
    // We exercise the same catch-up logic through create(), which calls processSingleTransaction
    // directly (no proxy) so it runs inside the test's transaction and sees all saved entities.

    @Test
    public void scheduler_transferWithSameAccount_createsDebit() {
        // Use yesterday so that after 1 monthly advance nextRunDate is clearly in the future
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        RecurringTransaction rt = buildRt(cal.getTime());
        rt.setTransactionType(TransactionType.TRANSFER);
        rt.setToAccountId(testAccount.getId()); // same account → falls back to DEBIT

        recurringTransactionService.create(testUser.getId(), rt);

        List<Transaction> txs = transactionRepository.findAll();
        assertEquals(1, txs.size());
        assertEquals(TransactionDbType.DEBIT, txs.get(0).getTransactionType());
    }

    @Test
    public void scheduler_transfer_createsBothLegs() {
        Account target = new Account();
        target.setName("Savings");
        target.setUserId(testUser.getId());
        target = accountRepository.save(target);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        RecurringTransaction rt = buildRt(cal.getTime());
        rt.setTransactionType(TransactionType.TRANSFER);
        rt.setToAccountId(target.getId());

        recurringTransactionService.create(testUser.getId(), rt);

        List<Transaction> txs = transactionRepository.findAll();
        assertEquals(2, txs.size());
        assertTrue(txs.stream().anyMatch(t -> t.getTransactionType() == TransactionDbType.DEBIT));
        assertTrue(txs.stream().anyMatch(t -> t.getTransactionType() == TransactionDbType.CREDIT));
    }

    @Test
    public void scheduler_nextRunDateAdvancedAfterBackfill() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = cal.getTime();

        RecurringTransaction saved = recurringTransactionService.create(testUser.getId(), buildRt(yesterday));

        assertNotNull(saved.getLastRunDate());
        cal.setTime(yesterday);
        cal.add(Calendar.MONTH, 1);
        assertEquals(cal.getTime().toString(), saved.getNextRunDate().toString());
    }

    // ==================== Bean Validation ====================

    @Test
    public void create_withZeroOriginalAmount_returnsBadRequest() throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("name", "Sub");
        p.put("accountId", testAccount.getId());
        p.put("categoryId", testCategory.getId());
        p.put("transactionType", TransactionType.DEBIT);
        p.put("frequency", "MONTHLY");
        Date now = new Date();
        p.put("startDate", now);
        p.put("nextRunDate", now);
        p.put("originalCurrency", "INR");
        p.put("originalAmount", 0.0); // @Positive fails
        p.put("exchangeRate", 1.0);

        mockMvc.perform(post("/api/recurring-transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void create_withZeroExchangeRate_returnsBadRequest() throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("name", "Sub");
        p.put("accountId", testAccount.getId());
        p.put("categoryId", testCategory.getId());
        p.put("transactionType", TransactionType.DEBIT);
        p.put("frequency", "MONTHLY");
        Date now = new Date();
        p.put("startDate", now);
        p.put("nextRunDate", now);
        p.put("originalCurrency", "INR");
        p.put("originalAmount", 10.0);
        p.put("exchangeRate", 0.0); // @Positive fails

        mockMvc.perform(post("/api/recurring-transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isBadRequest());
    }
}
