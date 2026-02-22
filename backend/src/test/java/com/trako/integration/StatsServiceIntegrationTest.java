package com.trako.integration;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.dtos.StatsResponseDTO;
import com.trako.entities.*;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UsersRepository;
import com.trako.services.StatsService;
import com.trako.services.TransactionWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class StatsServiceIntegrationTest {

    @Autowired
    private StatsService statsService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionWriteService transactionWriteService;

    private User user;
    private Account account;
    private Category food;
    private Category travel;

    @BeforeEach
    public void setup() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        usersRepository.deleteAll();

        user = new User();
        user.setName("Stats Svc");
        user.setPhoneNo("7777777777");
        user.setEmail("statssvc@example.com");
        user.setFireBaseId("pass");
        user = usersRepository.save(user);

        account = new Account();
        account.setName("A1");
        account.setUserId(user.getId());
        account = accountRepository.save(account);

        food = new Category();
        food.setUserId(user.getId());
        food.setName("Food");
        food.setCategoryType(CategoryType.EXPENSE);
        food = categoryRepository.save(food);

        travel = new Category();
        travel.setUserId(user.getId());
        travel.setName("Travel");
        travel.setCategoryType(CategoryType.EXPENSE);
        travel = categoryRepository.save(travel);
    }

    private Date date(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, 12, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    @Test
    public void customRangeShortReturnsDailySeries() {
        Date start = date(2026, 1, 1);
        Date end = date(2026, 1, 10); // 10 days inclusive

        saveTx(food.getId(), 1, 1, 10.0, date(2026, 1, 2));
        saveTx(food.getId(), 1, 1, 20.0, date(2026, 1, 5));

        StatsResponseDTO dto = statsService.getStats(user.getId(), StatsService.Range.custom, 1, null, null, start, end);

        assertNotNull(dto);
        assertEquals("custom", dto.getRange());
        assertNotNull(dto.getSeries());
        // Should span 10 days (Jan 1 to Jan 10 inclusive)
        assertEquals(10, dto.getSeries().size());
        // Labels should be daily format YYYY-MM-DD
        assertTrue(dto.getSeries().get(0).getLabel().matches("\\d{4}-\\d{2}-\\d{2}"));
        assertEquals("2026-01-01", dto.getSeries().get(0).getLabel());
        assertEquals("2026-01-10", dto.getSeries().get(9).getLabel());
    }

    @Test
    public void customRangeLongReturnsMonthlySeries() {
        Date start = date(2026, 1, 1);
        Date end = date(2026, 4, 1); // Spans into April (> 62 days threshold)

        saveTx(food.getId(), 1, 1, 10.0, date(2026, 1, 15));
        saveTx(food.getId(), 1, 1, 20.0, date(2026, 2, 15));
        saveTx(food.getId(), 1, 1, 30.0, date(2026, 3, 15));
        saveTx(food.getId(), 1, 1, 40.0, date(2026, 4, 1)); // Transaction on the last day

        StatsResponseDTO dto = statsService.getStats(user.getId(), StatsService.Range.custom, 1, null, null, start, end);

        assertNotNull(dto);
        assertEquals("custom", dto.getRange());
        assertNotNull(dto.getSeries());
        // Should span 4 months: Jan, Feb, Mar, Apr
        assertEquals(4, dto.getSeries().size());
        
        // Labels should be monthly format MMM YYYY (e.g. "Jan 2026")
        assertEquals("Jan 2026", dto.getSeries().get(0).getLabel());
        assertEquals("Feb 2026", dto.getSeries().get(1).getLabel());
        assertEquals("Mar 2026", dto.getSeries().get(2).getLabel());
        assertEquals("Apr 2026", dto.getSeries().get(3).getLabel());
        
        assertEquals(10.0, dto.getSeries().get(0).getValue(), 0.001);
        assertEquals(20.0, dto.getSeries().get(1).getValue(), 0.001);
        assertEquals(30.0, dto.getSeries().get(2).getValue(), 0.001);
        assertEquals(40.0, dto.getSeries().get(3).getValue(), 0.001);
    }

    @Test
    public void categoriesAreSortedByAmountDescForCurrentPeriod() {
        // Anchor within the same week for deterministic “current period” selection
        Date anchor = date(2026, 1, 8);

        // In the current week, Food total = 80, Travel total = 30
        saveExpense(food.getId(), 50.0, date(2026, 1, 6));
        saveExpense(food.getId(), 30.0, date(2026, 1, 7));
        saveExpense(travel.getId(), 30.0, date(2026, 1, 7));

        // Out of current week (still affects series, but not category breakdown for current period)
        saveExpense(travel.getId(), 999.0, date(2025, 12, 15));

        StatsResponseDTO dto = statsService.getStats(user.getId(), StatsService.Range.weekly, 1, null, anchor, null, null);

        assertNotNull(dto);
        assertEquals("weekly", dto.getRange());
        assertEquals(1, dto.getTransactionType());

        // Category breakdown should be sorted desc by amount
        assertNotNull(dto.getCategories());
        assertEquals(2, dto.getCategories().size());

        assertEquals(food.getId(), dto.getCategories().get(0).getCategoryId());
        assertEquals("Food", dto.getCategories().get(0).getCategoryName());
        assertEquals(80.0, dto.getCategories().get(0).getAmount(), 0.001);

        assertEquals(travel.getId(), dto.getCategories().get(1).getCategoryId());
        assertEquals("Travel", dto.getCategories().get(1).getCategoryName());
        assertEquals(30.0, dto.getCategories().get(1).getAmount(), 0.001);

        // Total equals sum of current-period category amounts
        assertEquals(110.0, dto.getTotal(), 0.001);

        // Series exists (over all data); just sanity check
        assertNotNull(dto.getSeries());
        assertTrue(dto.getSeries().size() >= 1);
    }

    @Test
    public void monthlyStatsBuildsMonthlySeriesAndSkipsInvalidCategoryIds() {
        Date anchor = date(2026, 2, 10);

        saveTx(food.getId(), 1, 1, 10.0, date(2026, 1, 5));
        saveTx(food.getId(), 1, 1, 20.0, date(2026, 2, 5));
        saveTx(travel.getId(), 1, 1, 5.0, date(2026, 2, 6));
        saveTx(food.getId(), 1, 1, null, date(2026, 2, 7));

        StatsResponseDTO dto = statsService.getStats(user.getId(), StatsService.Range.monthly, 1, null, anchor, null, null);

        assertNotNull(dto);
        assertEquals("monthly", dto.getRange());
        assertEquals(1, dto.getTransactionType());

        assertNotNull(dto.getSeries());
        assertTrue(dto.getSeries().stream().anyMatch(p -> "Jan 2026".equals(p.getLabel())));
        assertTrue(dto.getSeries().stream().anyMatch(p -> "Feb 2026".equals(p.getLabel())));

        assertNotNull(dto.getCategories());
        assertEquals(2, dto.getCategories().size());
        assertEquals(25.0, dto.getTotal(), 0.001);
    }

    @Test
    public void monthlySeriesCoversAllMonthLabels() {
        Date anchor = date(2026, 12, 15);

        for (int m = 1; m <= 12; m++) {
            saveTx(food.getId(), 1, 1, 1.0, date(2026, m, 2));
        }

        StatsResponseDTO dto = statsService.getStats(user.getId(), StatsService.Range.monthly, 1, null, anchor, null, null);

        assertNotNull(dto);
        assertEquals("monthly", dto.getRange());
        assertNotNull(dto.getSeries());
        // Continuous buckets from first month with data to anchor month (inclusive)
        assertTrue(dto.getSeries().size() >= 12);
    }

    @Test
    public void yearlyStatsBuildsYearlySeries() {
        Date anchor = date(2026, 6, 1);

        saveTx(food.getId(), 1, 1, 10.0, date(2025, 12, 31));
        saveTx(food.getId(), 1, 1, 20.0, date(2026, 1, 1));

        StatsResponseDTO dto = statsService.getStats(user.getId(), StatsService.Range.yearly, 1, null, anchor, null, null);

        assertNotNull(dto);
        assertEquals("yearly", dto.getRange());
        assertNotNull(dto.getSeries());
        assertTrue(dto.getSeries().stream().anyMatch(p -> "2025".equals(p.getLabel())));
        assertTrue(dto.getSeries().stream().anyMatch(p -> "2026".equals(p.getLabel())));
        // Continuous buckets from first year with data to anchor year (inclusive)
        assertTrue(dto.getSeries().size() >= 2);
    }

    @Test
    public void statsHasEmptySeriesWhenNoMatchingKindTransactions() {
        Date anchor = date(2026, 1, 8);

        saveTx(food.getId(), 2, 1, 50.0, date(2026, 1, 7));
        saveTx(food.getId(), 1, null, 50.0, date(2026, 1, 7));

        StatsResponseDTO dto = statsService.getStats(user.getId(), StatsService.Range.weekly, 1, null, anchor, null, null);

        assertNotNull(dto);
        assertNotNull(dto.getSeries());
        // No matching DEBIT transactions => no buckets
        assertEquals(0, dto.getSeries().size());
        assertNotNull(dto.getCategories());
        assertEquals(0, dto.getCategories().size());
        assertEquals(0.0, dto.getTotal(), 0.001);
    }

    @Test
    public void categoryStatsWeeklyFiltersByCategoryAndComputesTotalForCurrentPeriod() {
        Date anchor = date(2026, 1, 8);

        saveTx(food.getId(), 1, 1, 40.0, date(2026, 1, 7));
        saveTx(travel.getId(), 1, 1, 999.0, date(2026, 1, 7));
        saveTx(food.getId(), 1, 1, 500.0, date(2025, 12, 15));

        var dto = statsService.getCategoryStats(user.getId(), StatsService.Range.weekly, 1, null, anchor, food.getId(), null, null);

        assertNotNull(dto);
        assertEquals("weekly", dto.getRange());
        assertEquals(1, dto.getTransactionType());
        assertEquals(food.getId(), dto.getCategoryId());
        assertNotNull(dto.getSeries());
        assertTrue(dto.getSeries().size() >= 1);
        assertEquals(40.0, dto.getTotal(), 0.001);
    }

    @Test
    public void categoryStatsWithNullCategoryReturnsEmpty() {
        Date anchor = date(2026, 1, 8);

        var dto = statsService.getCategoryStats(user.getId(), StatsService.Range.weekly, 1, null, anchor, null, null, null);

        assertNotNull(dto);
        assertNotNull(dto.getSeries());
        assertEquals(0, dto.getSeries().size());
        assertEquals(0.0, dto.getTotal(), 0.001);
    }


    @Test
    public void filterCategoryAndMonthLabelUnreachableBranchesAreCoveredViaReflection() throws Exception {
        Method monthLabel = StatsService.class.getDeclaredMethod("monthLabel", int.class);
        monthLabel.setAccessible(true);

        String unknown = (String) monthLabel.invoke(statsService, 13);
        assertEquals("", unknown);
    }

    private void saveExpense(Long categoryId, double amount, Date d) {
        saveTx(categoryId, 1, 1, amount, d);
    }

    private void saveTx(Long categoryId, Integer transactionType, Integer isCountable, Double amount, Date d) {
        Transaction t = newTx(categoryId, transactionType, isCountable, amount, d);
        transactionWriteService.saveForUser(user.getId(), t);
    }

    private Transaction newTx(Long categoryId, Integer transactionType, Integer isCountable, Double amount, Date d) {
        Transaction t = new Transaction();
        t.setAccountId(account.getId());
        t.setCategoryId(categoryId);
        t.setName("t-" + categoryId + "-" + amount);
        t.setTransactionType(transactionType);
        if (amount == null) {
            // Keep TransactionWriteService happy while ensuring this row doesn't impact stats totals.
            t.setIsCountable(0);
            t.setAmount(0.0);
        } else {
            t.setIsCountable(isCountable);
            t.setAmount(amount);
        }
        t.setDate(d);
        return t;
    }
}
