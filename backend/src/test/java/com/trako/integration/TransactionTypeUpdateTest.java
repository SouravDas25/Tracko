package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.*;
import com.trako.repositories.*;
import com.trako.services.TransactionWriteService;
import com.trako.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionTypeUpdateTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsersRepository usersRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private TransactionWriteService transactionWriteService;
    @Autowired private JwtTokenUtil jwtTokenUtil;

    private User testUser;
    private String bearerToken;
    private Account account1;
    private Account account2;
    private Category categoryExpense;
    private Category categoryIncome;

    @BeforeEach
    public void setup() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        usersRepository.deleteAll();

        testUser = new User();
        testUser.setName("User");
        testUser.setPhoneNo("9998887777");
        testUser.setEmail("user@example.com");
        testUser.setPassword("pass");
        testUser.setBaseCurrency("INR");
        testUser = usersRepository.save(testUser);

        var principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(), testUser.getPassword(), Collections.emptyList());
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);

        account1 = new Account(); account1.setName("A1"); account1.setUserId(testUser.getId()); account1.setCurrency("INR"); account1 = accountRepository.save(account1);
        account2 = new Account(); account2.setName("A2"); account2.setUserId(testUser.getId()); account2.setCurrency("INR"); account2 = accountRepository.save(account2);

        categoryExpense = new Category(); categoryExpense.setName("ExpenseCat"); categoryExpense.setUserId(testUser.getId()); categoryExpense.setCategoryType(CategoryType.EXPENSE); categoryExpense = categoryRepository.save(categoryExpense);
        categoryIncome = new Category(); categoryIncome.setName("IncomeCat"); categoryIncome.setUserId(testUser.getId()); categoryIncome.setCategoryType(CategoryType.INCOME); categoryIncome = categoryRepository.save(categoryIncome);
    }

    @Test
    public void testUpdateExpenseToIncome() throws Exception {
        // Create Expense (DEBIT)
        Transaction t = new Transaction();
        t.setTransactionType(TransactionType.DEBIT);
        t.setName("Expense");
        t.setOriginalAmount(100.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setDate(new Date());
        t.setAccountId(account1.getId());
        t.setCategoryId(categoryExpense.getId());
        t.setIsCountable(1);
        t = transactionWriteService.saveForUser(testUser.getId(), t);

        // Update to Income (CREDIT)
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionType", TransactionType.CREDIT);
        payload.put("categoryId", categoryIncome.getId());
        payload.put("name", "Now Income");

        mockMvc.perform(put("/api/transactions/" + t.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        Transaction updated = transactionRepository.findById(t.getId()).orElseThrow();
        assertThat(updated.getTransactionType()).isEqualTo(TransactionType.CREDIT);
        assertThat(updated.getCategoryId()).isEqualTo(categoryIncome.getId());
        assertThat(updated.getName()).isEqualTo("Now Income");
    }

    @Test
    public void testUpdateIncomeToExpense() throws Exception {
        // Create Income (CREDIT)
        Transaction t = new Transaction();
        t.setTransactionType(TransactionType.CREDIT);
        t.setName("Income");
        t.setOriginalAmount(500.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setDate(new Date());
        t.setAccountId(account1.getId());
        t.setCategoryId(categoryIncome.getId());
        t.setIsCountable(1);
        t = transactionWriteService.saveForUser(testUser.getId(), t);

        // Update to Expense (DEBIT)
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionType", TransactionType.DEBIT);
        payload.put("categoryId", categoryExpense.getId());
        payload.put("name", "Now Expense");

        mockMvc.perform(put("/api/transactions/" + t.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        Transaction updated = transactionRepository.findById(t.getId()).orElseThrow();
        assertThat(updated.getTransactionType()).isEqualTo(TransactionType.DEBIT);
        assertThat(updated.getCategoryId()).isEqualTo(categoryExpense.getId());
        assertThat(updated.getName()).isEqualTo("Now Expense");
    }

    @Test
    public void testUpdateExpenseToTransfer() throws Exception {
        // Create Expense (DEBIT)
        Transaction t = new Transaction();
        t.setTransactionType(TransactionType.DEBIT);
        t.setName("Expense");
        t.setOriginalAmount(100.0);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setDate(new Date());
        t.setAccountId(account1.getId());
        t.setCategoryId(categoryExpense.getId());
        t.setIsCountable(1);
        t = transactionWriteService.saveForUser(testUser.getId(), t);

        // Update to Transfer (to Account 2)
        // We need to provide toAccountId to signal it's a transfer
        Map<String, Object> payload = new HashMap<>();
        payload.put("toAccountId", account2.getId());
        payload.put("name", "Now Transfer");

        mockMvc.perform(put("/api/transactions/" + t.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        Transaction updated = transactionRepository.findById(t.getId()).orElseThrow();
        assertThat(updated.getLinkedTransactionId()).isNotNull();
        assertThat(updated.getName()).isEqualTo("Now Transfer");
        assertThat(updated.getIsCountable()).isEqualTo(0); // Transfers should not be countable

        // Verify linked transaction exists
        Transaction linked = transactionRepository.findById(updated.getLinkedTransactionId()).orElseThrow();
        assertThat(linked.getAccountId()).isEqualTo(account2.getId());
        assertThat(linked.getTransactionType()).isEqualTo(TransactionType.CREDIT); // Other side of transfer
        assertThat(linked.getIsCountable()).isEqualTo(0);
        
        // Verify Category is implicitly changed to TRANSFER
        List<Category> transferCats = categoryRepository.findByUserIdAndName(testUser.getId(), "TRANSFER");
        assertThat(transferCats).isNotEmpty();
        Long transferCatId = transferCats.get(0).getId();
        assertThat(updated.getCategoryId()).isEqualTo(transferCatId);
        assertThat(linked.getCategoryId()).isEqualTo(transferCatId);
    }

    @Test
    public void testUpdateTransferToExpense() throws Exception {
        // Create Transfer (Account 1 -> Account 2)
        Transaction[] transfer = transactionWriteService.createTransfer(
                testUser.getId(),
                account1.getId(),
                account2.getId(),
                new Date(),
                200.0,
                "INR",
                1.0,
                "Transfer",
                "Comments"
        );
        Transaction debitSide = transfer[0];
        
        // Update to Regular Expense (DEBIT)
        // We signal conversion to regular by ... ? 
        // Current API might not have a clear signal if we just omit toAccountId.
        // But for this test, let's assume we want to "break" the transfer.
        // Maybe by setting linkedTransactionId to null? Or just updating it as a regular transaction?
        // If the controller logic sees it's an existing transfer, it enforces transfer update.
        // We might need a flag or specific logic. 
        // For now, let's try to update it as if it's a regular transaction (no toAccountId).
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("transactionType", TransactionType.DEBIT);
        payload.put("categoryId", categoryExpense.getId());
        payload.put("name", "Converted to Expense");
        // Explicitly NOT sending toAccountId.

        mockMvc.perform(put("/api/transactions/" + debitSide.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        Transaction updated = transactionRepository.findById(debitSide.getId()).orElseThrow();
        assertThat(updated.getLinkedTransactionId()).isNull(); // Should be unlinked
        assertThat(updated.getTransactionType()).isEqualTo(TransactionType.DEBIT);
        assertThat(updated.getName()).isEqualTo("Converted to Expense");
        assertThat(updated.getCategoryId()).isEqualTo(categoryExpense.getId());
        assertThat(updated.getIsCountable()).isEqualTo(1); // Should be countable again by default

        // Verify the OTHER side is deleted or handled?
        // Ideally, if we convert Transfer -> Expense, the other side (Credit) should be deleted.
        // Let's check if the credit side still exists.
        boolean creditExists = transactionRepository.existsById(transfer[1].getId());
        assertThat(creditExists).isFalse();
    }
}
