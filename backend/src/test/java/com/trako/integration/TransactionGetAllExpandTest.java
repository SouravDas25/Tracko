package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.*;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UsersRepository;
import com.trako.services.transactions.TransactionWriteService;
import com.trako.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionGetAllExpandTest extends BaseIntegrationTest {

    @Autowired
    private TransactionWriteService transactionWriteService;

    private String bearerToken;
    private User user;
    private Account acc1;
    private Account acc2;
    private Category food;
    private Category transferCat;

    @BeforeEach
    public void setup() {
        user = createUniqueUser("U1");
        bearerToken = generateBearerToken(user);

        acc1 = new Account();
        acc1.setName("A1");
        acc1.setUserId(user.getId());
        acc1 = accountRepository.save(acc1);
        acc2 = new Account();
        acc2.setName("A2");
        acc2.setUserId(user.getId());
        acc2 = accountRepository.save(acc2);

        food = new Category();
        food.setName("Food");
        food.setUserId(user.getId());
        food = categoryRepository.save(food);
        // Ensure TRANSFER category exists for hiding
        transferCat = new Category();
        transferCat.setName("TRANSFER");
        transferCat.setUserId(user.getId());
        transferCat = categoryRepository.save(transferCat);

        // Create transactions in current month
        Date now = new Date();
        Transaction t1 = new Transaction();
        t1.setTransactionType(TransactionEntryType.DEBIT);
        t1.setName("Lunch");
        t1.setOriginalAmount(50.0);
        t1.setOriginalCurrency("INR");
        t1.setExchangeRate(1.0);
        t1.setDate(now);
        t1.setAccountId(acc1.getId());
        t1.setCategoryId(food.getId());
        t1.setIsCountable(1);
        transactionWriteService.saveForUser(user.getId(), t1);

        // Create a transfer pair: debit (countable=0) + credit (countable=0) under TRANSFER
        Transaction debit = new Transaction();
        debit.setTransactionType(TransactionEntryType.DEBIT);
        debit.setName("Xfer Out");
        debit.setOriginalAmount(100.0);
        debit.setOriginalCurrency("INR");
        debit.setExchangeRate(1.0);
        debit.setDate(now);
        debit.setAccountId(acc1.getId());
        debit.setCategoryId(transferCat.getId());
        debit.setIsCountable(0);
        debit = transactionWriteService.saveForUser(user.getId(), debit);

        Transaction credit = new Transaction();
        credit.setTransactionType(TransactionEntryType.CREDIT);
        credit.setName("Xfer In");
        credit.setOriginalAmount(100.0);
        credit.setOriginalCurrency("INR");
        credit.setExchangeRate(1.0);
        credit.setDate(now);
        credit.setAccountId(acc2.getId());
        credit.setCategoryId(transferCat.getId());
        credit.setIsCountable(0);
        credit.setLinkedTransactionId(debit.getId());
        credit = transactionWriteService.saveForUser(user.getId(), credit);

        debit.setLinkedTransactionId(credit.getId());
        transactionWriteService.saveForUser(user.getId(), debit);
    }

    @Test
    public void expandTrue_noCategory_showsDTOsAndHidesTransferCredit() throws Exception {
        Calendar cal = Calendar.getInstance();
        int m = cal.get(Calendar.MONTH) + 1;
        int y = cal.get(Calendar.YEAR);

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", String.valueOf(m))
                        .param("year", String.valueOf(y))
                        .param("expand", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactions", notNullValue()))
                // Should hide transfer credit side in DTOs
                .andExpect(jsonPath("$.result.transactions[*].name", not(hasItem("Xfer In"))));
    }

    @Test
    public void expandTrue_withCategory_filtersByCategory() throws Exception {
        Calendar cal = Calendar.getInstance();
        int m = cal.get(Calendar.MONTH) + 1;
        int y = cal.get(Calendar.YEAR);

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", String.valueOf(m))
                        .param("year", String.valueOf(y))
                        .param("expand", "true")
                        .param("categoryId", String.valueOf(food.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactions[*].name", hasItem("Lunch")));
    }

    @Test
    public void expandTrue_withAccountIds_filtersByAccounts() throws Exception {
        Calendar cal = Calendar.getInstance();
        int m = cal.get(Calendar.MONTH) + 1;
        int y = cal.get(Calendar.YEAR);

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", String.valueOf(m))
                        .param("year", String.valueOf(y))
                        .param("expand", "true")
                        .param("accountIds", String.valueOf(acc1.getId())))
                .andExpect(status().isOk())
                // Only acc1 items present (Lunch and Xfer Out DTO)
                .andExpect(jsonPath("$.result.transactions[*].accountId", everyItem(is(acc1.getId().intValue()))));
    }
}
