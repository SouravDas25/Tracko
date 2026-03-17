package com.trako.integration.analytics;

import com.trako.dtos.AnalyticsResponseDTO;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.enums.AnalyticsGranularity;
import com.trako.enums.AnalyticsGroupBy;
import com.trako.enums.CategoryType;
import com.trako.enums.TransactionDbType;
import com.trako.enums.TransactionType;
import com.trako.integration.BaseIntegrationTest;
import com.trako.services.AnalyticsService;
import com.trako.services.transactions.TransactionWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AnalyticsServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private TransactionWriteService transactionWriteService;

    private User user;
    private Account account;
    private Category food;

    @BeforeEach
    public void setup() {
        user = createUniqueUser("AnalyticsSvcUser");

        account = new Account();
        account.setName("TestAccount");
        account.setUserId(user.getId());
        account = accountRepository.save(account);

        food = new Category();
        food.setUserId(user.getId());
        food.setName("Food");
        food.setCategoryType(CategoryType.EXPENSE);
        food = categoryRepository.save(food);
    }

    private Date date(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, 12, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private void saveTx(double amount, Date date) {
        Transaction t = new Transaction();
        t.setAccountId(account.getId());
        t.setCategoryId(food.getId());
        t.setName("Tx " + amount);
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setIsCountable(1);
        t.setOriginalAmount(amount);
        t.setOriginalCurrency("USD");
        t.setExchangeRate(1.0);
        t.setDate(date);
        transactionWriteService.saveForUser(user.getId(), t);
    }

    @Test
    public void testDateFiltering_OnlyIncludesTransactionsInRange() {
        // Range: Jan 1 to Jan 31
        Date start = date(2026, 1, 1);
        Date end = date(2026, 1, 31);

        // Inside range
        saveTx(100.0, date(2026, 1, 15));
        
        // Outside range (before)
        saveTx(50.0, date(2025, 12, 31));
        
        // Outside range (after)
        saveTx(75.0, date(2026, 2, 1));

        AnalyticsResponseDTO result = analyticsService.getChartData(
                user.getId(),
                TransactionType.DEBIT,
                start,
                end,
                AnalyticsGranularity.MONTHLY,
                null, // No grouping
                null,
                null
        );

        assertNotNull(result);
        // Should only sum the 100.0 inside the range
        assertEquals(100.0, result.getTotal(), 0.001);
        assertEquals(1, result.getGroupedSeries().size());
        assertEquals("All", result.getGroupedSeries().get(0).getName());
    }

    @Test
    public void testDateFiltering_GroupedByCategory() {
        // Range: Jan 1 to Jan 31
        Date start = date(2026, 1, 1);
        Date end = date(2026, 1, 31);

        saveTx(200.0, date(2026, 1, 10)); // Inside
        saveTx(300.0, date(2026, 2, 10)); // Outside

        AnalyticsResponseDTO result = analyticsService.getChartData(
                user.getId(),
                TransactionType.DEBIT,
                start,
                end,
                AnalyticsGranularity.MONTHLY,
                AnalyticsGroupBy.CATEGORY,
                null,
                null
        );

        assertEquals(200.0, result.getTotal(), 0.001);
        // Only one category should be returned (Food) because the other tx is out of range
        // If the outside tx was included, we might see it or see a higher total
    }
}
