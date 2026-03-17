package com.trako.integration.transaction;

import com.trako.dtos.TransferResult;
import com.trako.entities.*;
import com.trako.enums.TransactionDbType;
import com.trako.enums.TransactionType;
import com.trako.models.request.TransactionRequest;
import com.trako.integration.BaseIntegrationTest;
import com.trako.repositories.ContactRepository;
import com.trako.repositories.SplitRepository;
import com.trako.repositories.UserCurrencyRepository;
import com.trako.services.transactions.TransactionWriteService;
import com.trako.services.transactions.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TransactionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TransactionWriteService transactionWriteService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private SplitRepository splitRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private UserCurrencyRepository userCurrencyRepository;

    private User testUser;
    private Account testAccount;
    private Category testCategory;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser();
        bearerToken = generateBearerToken(testUser);

        testAccount = new Account();
        testAccount.setName("Savings");
        testAccount.setUserId(testUser.getId());
        testAccount = accountRepository.save(testAccount);

        testCategory = new Category();
        testCategory.setName("Food");
        testCategory.setUserId(testUser.getId());
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    public void testCreateTransaction() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT);
        transaction.setName("Lunch");
        transaction.setOriginalAmount(25.50);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        transaction.setComments("Pizza");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Lunch"))
                .andExpect(jsonPath("$.result.amount").value(25.50))
                .andExpect(jsonPath("$.result.comments").value("Pizza"));
    }

    @Test
    public void testCreateTransfer_usesProvidedDateForBothSides() throws Exception {
        Account toAccount = new Account();
        toAccount.setName("Checking");
        toAccount.setUserId(testUser.getId());
        toAccount = accountRepository.save(toAccount);

        Date transferDate = new GregorianCalendar(2020, Calendar.JANUARY, 15).getTime();

        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                testAccount.getId(),     // accountId (source)
                transferDate,            // date
                "My Transfer",          // name
                "date-check",           // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "INR",                   // originalCurrency
                123.45,                  // originalAmount
                null,                    // exchangeRate (auto-resolve to 1.0 for base currency)
                null,                    // linkedTransactionId
                toAccount.getId(),        // toAccountId
                null                     // fromAccountId
        );

        var mvcResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").exists())
                .andReturn();

        var root = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        long debitId = root.path("result").path("id").asLong();

        Transaction debit = transactionRepository.findById(debitId).orElseThrow();
        assertNotNull(debit.getLinkedTransactionId(), "Debit side must link to credit transaction");

        Transaction credit = transactionRepository.findById(debit.getLinkedTransactionId()).orElseThrow();

        assertNotNull(debit.getDate());
        assertNotNull(credit.getDate());
        assertEquals(transferDate.getTime(), debit.getDate().getTime(), "Debit date must match provided transfer date");
        assertEquals(transferDate.getTime(), credit.getDate().getTime(), "Credit date must match provided transfer date");
    }

    @Test
    public void testUpdateTransferDate_updatesBothSides() throws Exception {
        Account toAccount = new Account();
        toAccount.setName("Wallet");
        toAccount.setUserId(testUser.getId());
        toAccount = accountRepository.save(toAccount);

        Date initialDate = new GregorianCalendar(2020, Calendar.FEBRUARY, 1).getTime();
        TransferResult created = transferService.createTransfer(
                testUser.getId(),
                testAccount.getId(),
                toAccount.getId(),
                initialDate,
                50.00,
                "INR",
                1.0,
                "Init Transfer",
                "init"
        );

        Transaction debit = created.debit();
        assertNotNull(debit.getId());
        assertNotNull(debit.getLinkedTransactionId());

        Date newDate = new GregorianCalendar(2021, Calendar.MARCH, 10).getTime();

        TransactionRequest updatePayload = new TransactionRequest(
                null,                    // id
                debit.getAccountId(),    // accountId
                newDate,                 // date
                "Updated Transfer",     // name
                "updated",              // comments
                null,                    // categoryId
                TransactionType.TRANSFER, // transactionType - updating a transfer, not converting
                debit.getOriginalCurrency(), // originalCurrency
                debit.getOriginalAmount(),   // originalAmount
                null,                    // exchangeRate (keep existing)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(put("/api/transactions/{id}", debit.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk());

        Transaction updatedDebit = transactionRepository.findById(debit.getId()).orElseThrow();
        Transaction updatedCredit = transactionRepository.findById(updatedDebit.getLinkedTransactionId()).orElseThrow();

        assertEquals(newDate.getTime(), updatedDebit.getDate().getTime(), "Debit date must update to new date");
        assertEquals(newDate.getTime(), updatedCredit.getDate().getTime(), "Credit date must update to new date");
    }

    @Test
    public void testGetAllTransactions() throws Exception {
        Transaction transaction1 = new Transaction();
        transaction1.setTransactionType(TransactionDbType.DEBIT);
        transaction1.setName("Lunch");
        transaction1.setOriginalAmount(25.50);
        transaction1.setOriginalCurrency("INR");
        transaction1.setExchangeRate(1.0);
        transaction1.setDate(new Date());
        transaction1.setAccountId(testAccount.getId());
        transaction1.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), transaction1);

        Transaction transaction2 = new Transaction();
        transaction2.setTransactionType(TransactionDbType.DEBIT);
        transaction2.setName("Dinner");
        transaction2.setOriginalAmount(35.00);
        transaction2.setOriginalCurrency("INR");
        transaction2.setExchangeRate(1.0);
        transaction2.setDate(new Date());
        transaction2.setAccountId(testAccount.getId());
        transaction2.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), transaction2);

        Calendar now = Calendar.getInstance();
        String month = String.valueOf(now.get(Calendar.MONTH) + 1);
        String year = String.valueOf(now.get(Calendar.YEAR));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", month)
                        .param("year", year))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactions", hasSize(2)))
                .andExpect(jsonPath("$.result.transactions[*].name", containsInAnyOrder("Lunch", "Dinner")));
    }

    @Test
    public void testGetAllTransactions_expandTrue_returnsDetailsWithSplitsAndContact() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT);
        transaction.setName("Dinner");
        transaction.setOriginalAmount(100.00);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), transaction);

        Contact contact = new Contact();
        contact.setUserId(testUser.getId());
        contact.setName("Alice");
        contact.setPhoneNo("9990001111");
        contact = contactRepository.save(contact);

        Split split = new Split();
        split.setTransactionId(saved.getId());
        split.setUserId(testUser.getId());
        split.setContactId(contact.getId());
        split.setAmount(40.00);
        split.setIsSettled(0);
        splitRepository.save(split);

        Split splitNoContact = new Split();
        splitNoContact.setTransactionId(saved.getId());
        splitNoContact.setUserId(testUser.getId());
        splitNoContact.setContactId(null);
        splitNoContact.setAmount(60.00);
        splitNoContact.setIsSettled(0);
        splitRepository.save(splitNoContact);

        Calendar now = Calendar.getInstance();
        String month = String.valueOf(now.get(Calendar.MONTH) + 1);
        String year = String.valueOf(now.get(Calendar.YEAR));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", month)
                        .param("year", year)
                        .param("expand", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactions", hasSize(1)))
                .andExpect(jsonPath("$.result.transactions[0].id").value(saved.getId().intValue()))
                .andExpect(jsonPath("$.result.transactions[0].accountId").value(testAccount.getId().intValue()))
                .andExpect(jsonPath("$.result.transactions[0].categoryId").value(testCategory.getId().intValue()))
                .andExpect(jsonPath("$.result.transactions[0].account.id").value(testAccount.getId().intValue()))
                .andExpect(jsonPath("$.result.transactions[0].category.id").value(testCategory.getId().intValue()))
                .andExpect(jsonPath("$.result.transactions[0].splits", hasSize(2)))
                .andExpect(jsonPath("$.result.transactions[0].splits[0].split.amount").value(40.00))
                .andExpect(jsonPath("$.result.transactions[0].splits[0].contact.id").value(contact.getId().intValue()))
                .andExpect(jsonPath("$.result.transactions[0].splits[0].contact.name").value("Alice"));
    }

    @Test
    public void testGetAllTransactions_expandTrue_withCategoryId_usesDetailsCategoryPath() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT);
        transaction.setName("CatDinner");
        transaction.setOriginalAmount(55.00);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), transaction);

        Calendar now = Calendar.getInstance();
        String month = String.valueOf(now.get(Calendar.MONTH) + 1);
        String year = String.valueOf(now.get(Calendar.YEAR));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", month)
                        .param("year", year)
                        .param("categoryId", String.valueOf(testCategory.getId()))
                        .param("expand", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactions", hasSize(1)))
                .andExpect(jsonPath("$.result.transactions[0].id").value(saved.getId().intValue()))
                .andExpect(jsonPath("$.result.transactions[0].category.id").value(testCategory.getId().intValue()))
                .andExpect(jsonPath("$.result.transactions[0].account.id").value(testAccount.getId().intValue()));
    }

    @Test
    public void testGetAllTransactions_expandTrue_whenEmpty_returnsEmptyTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", "1")
                        .param("year", "1990")
                        .param("expand", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactions", hasSize(0)))
                .andExpect(jsonPath("$.result.totalElements").value(0));

    }

    @Test
    public void testGetTransactionById() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT); // DEBIT = expense
        transaction.setName("Coffee");
        transaction.setOriginalAmount(5.00);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), transaction);

        mockMvc.perform(get("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Coffee"))
                .andExpect(jsonPath("$.result.amount").value(5.00));
    }

    @Test
    public void testUpdateTransaction() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT);
        transaction.setName("Old Name");
        transaction.setOriginalAmount(10.00);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), transaction);

        saved.setName("Updated Name");
        saved.setOriginalAmount(15.00);
        saved.setOriginalCurrency("INR");
        saved.setExchangeRate(1.0);

        mockMvc.perform(put("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Updated Name"))
                .andExpect(jsonPath("$.result.amount").value(15.00));
    }

    @Test
    public void testDeleteTransaction() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT);
        transaction.setName("To Delete");
        transaction.setOriginalAmount(1.00);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), transaction);

        mockMvc.perform(delete("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetSummary() throws Exception {
        // Create income transaction
        Transaction income = new Transaction();
        income.setTransactionType(TransactionDbType.CREDIT); // CREDIT = income
        income.setName("Salary");
        income.setOriginalAmount(1000.00);
        income.setOriginalCurrency("INR");
        income.setExchangeRate(1.0);
        income.setDate(new Date());
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        // Create expense transaction
        Transaction expense = new Transaction();
        expense.setTransactionType(TransactionDbType.DEBIT); // DEBIT = expense
        expense.setName("Groceries");
        expense.setOriginalAmount(200.00);
        expense.setOriginalCurrency("INR");
        expense.setExchangeRate(1.0);
        expense.setDate(new Date());
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(testCategory.getId());
        expense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), expense);

        // Create non-countable transaction (should be excluded)
        Transaction nonCountable = new Transaction();
        nonCountable.setTransactionType(TransactionDbType.DEBIT); // DEBIT = expense
        nonCountable.setName("Transfer");
        nonCountable.setOriginalAmount(50.00);
        nonCountable.setOriginalCurrency("INR");
        nonCountable.setExchangeRate(1.0);
        nonCountable.setDate(new Date());
        nonCountable.setAccountId(testAccount.getId());
        nonCountable.setCategoryId(testCategory.getId());
        nonCountable.setIsCountable(0);
        transactionWriteService.saveForUser(testUser.getId(), nonCountable);

        mockMvc.perform(get("/api/transactions/summary")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalIncome").value(1000.00))
                .andExpect(jsonPath("$.result.totalExpense").value(200.00))
                .andExpect(jsonPath("$.result.netTotal").value(800.00))
                .andExpect(jsonPath("$.result.transactionCount").value(2));
    }

    @Test
    public void testGetSummary_singleAccountFilter_stillExcludesTransfers() throws Exception {
        Account toAccount = new Account();
        toAccount.setName("Checking");
        toAccount.setUserId(testUser.getId());
        toAccount = accountRepository.save(toAccount);

        Date txDate = new GregorianCalendar(2020, Calendar.JANUARY, 15).getTime();

        Transaction income = new Transaction();
        income.setTransactionType(TransactionDbType.CREDIT);
        income.setName("Salary");
        income.setOriginalAmount(1000.00);
        income.setOriginalCurrency("INR");
        income.setExchangeRate(1.0);
        income.setDate(txDate);
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        Transaction expense = new Transaction();
        expense.setTransactionType(TransactionDbType.DEBIT);
        expense.setName("Groceries");
        expense.setOriginalAmount(200.00);
        expense.setOriginalCurrency("INR");
        expense.setExchangeRate(1.0);
        expense.setDate(txDate);
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(testCategory.getId());
        expense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), expense);

        transferService.createTransfer(
                testUser.getId(),
                testAccount.getId(),
                toAccount.getId(),
                txDate,
                50.00,
                "INR",
                1.0,
                "Xfer",
                ""
        );

        mockMvc.perform(get("/api/transactions/summary")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2020-02-01")
                        .param("accountIds", String.valueOf(testAccount.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalIncome").value(1000.00))
                .andExpect(jsonPath("$.result.totalExpense").value(200.00))
                .andExpect(jsonPath("$.result.netTotal").value(800.00))
                .andExpect(jsonPath("$.result.transactionCount").value(2));
    }

    @Test
    public void testGetAccountSummary_includesTransferDeltaInNetOnly() throws Exception {
        Account toAccount = new Account();
        toAccount.setName("Checking");
        toAccount.setUserId(testUser.getId());
        toAccount = accountRepository.save(toAccount);

        Date txDate = new GregorianCalendar(2020, Calendar.JANUARY, 15).getTime();

        Transaction income = new Transaction();
        income.setTransactionType(TransactionDbType.CREDIT);
        income.setName("Salary");
        income.setOriginalAmount(1000.00);
        income.setOriginalCurrency("INR");
        income.setExchangeRate(1.0);
        income.setDate(txDate);
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        Transaction expense = new Transaction();
        expense.setTransactionType(TransactionDbType.DEBIT);
        expense.setName("Groceries");
        expense.setOriginalAmount(200.00);
        expense.setOriginalCurrency("INR");
        expense.setExchangeRate(1.0);
        expense.setDate(txDate);
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(testCategory.getId());
        expense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), expense);

        transferService.createTransfer(
                testUser.getId(),
                testAccount.getId(),
                toAccount.getId(),
                txDate,
                50.00,
                "INR",
                1.0,
                "Xfer",
                ""
        );

        mockMvc.perform(get("/api/accounts/{accountId}/summary", testAccount.getId())
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2020-02-01")
                        .param("includeRollover", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalIncome").value(1000.00))
                .andExpect(jsonPath("$.result.totalExpense").value(200.00))
                .andExpect(jsonPath("$.result.netTotal").value(750.00))
                .andExpect(jsonPath("$.result.transactionCount").value(2));
    }

    @Test
    public void testGetTotalIncome() throws Exception {
        Transaction income1 = new Transaction();
        income1.setTransactionType(TransactionDbType.CREDIT); // CREDIT = income
        income1.setName("Salary");
        income1.setOriginalAmount(1000.00);
        income1.setOriginalCurrency("INR");
        income1.setExchangeRate(1.0);
        income1.setDate(new Date());
        income1.setAccountId(testAccount.getId());
        income1.setCategoryId(testCategory.getId());
        income1.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income1);

        Transaction income2 = new Transaction();
        income2.setTransactionType(TransactionDbType.CREDIT); // CREDIT = income
        income2.setName("Bonus");
        income2.setOriginalAmount(500.00);
        income2.setOriginalCurrency("INR");
        income2.setExchangeRate(1.0);
        income2.setDate(new Date());
        income2.setAccountId(testAccount.getId());
        income2.setCategoryId(testCategory.getId());
        income2.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income2);

        mockMvc.perform(get("/api/transactions/total-income")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(1500.00));
    }

    @Test
    public void testGetTotalExpense() throws Exception {
        Transaction expense1 = new Transaction();
        expense1.setTransactionType(TransactionDbType.DEBIT); // DEBIT = expense
        expense1.setName("Groceries");
        expense1.setOriginalAmount(200.00);
        expense1.setOriginalCurrency("INR");
        expense1.setExchangeRate(1.0);
        expense1.setDate(new Date());
        expense1.setAccountId(testAccount.getId());
        expense1.setCategoryId(testCategory.getId());
        expense1.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), expense1);

        Transaction expense2 = new Transaction();
        expense2.setTransactionType(TransactionDbType.DEBIT); // DEBIT = expense
        expense2.setName("Utilities");
        expense2.setOriginalAmount(150.00);
        expense2.setOriginalCurrency("INR");
        expense2.setExchangeRate(1.0);
        expense2.setDate(new Date());
        expense2.setAccountId(testAccount.getId());
        expense2.setCategoryId(testCategory.getId());
        expense2.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), expense2);

        mockMvc.perform(get("/api/transactions/total-expense")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(350.00));
    }

    @Test
    public void testGetSummaryExcludesNonCountable() throws Exception {
        // Only non-countable transactions
        Transaction nonCountable1 = new Transaction();
        nonCountable1.setTransactionType(TransactionDbType.CREDIT); // CREDIT = income
        nonCountable1.setName("Transfer In");
        nonCountable1.setOriginalAmount(500.00);
        nonCountable1.setOriginalCurrency("INR");
        nonCountable1.setExchangeRate(1.0);
        nonCountable1.setDate(new Date());
        nonCountable1.setAccountId(testAccount.getId());
        nonCountable1.setCategoryId(testCategory.getId());
        nonCountable1.setIsCountable(0);
        transactionWriteService.saveForUser(testUser.getId(), nonCountable1);

        Transaction nonCountable2 = new Transaction();
        nonCountable2.setTransactionType(TransactionDbType.DEBIT); // DEBIT = expense
        nonCountable2.setName("Transfer Out");
        nonCountable2.setOriginalAmount(300.00);
        nonCountable2.setOriginalCurrency("INR");
        nonCountable2.setExchangeRate(1.0);
        nonCountable2.setDate(new Date());
        nonCountable2.setAccountId(testAccount.getId());
        nonCountable2.setCategoryId(testCategory.getId());
        nonCountable2.setIsCountable(0);
        transactionWriteService.saveForUser(testUser.getId(), nonCountable2);

        mockMvc.perform(get("/api/transactions/summary")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalIncome").value(0.00))
                .andExpect(jsonPath("$.result.totalExpense").value(0.00))
                .andExpect(jsonPath("$.result.netTotal").value(0.00))
                .andExpect(jsonPath("$.result.transactionCount").value(0));
    }

    @Test
    public void testCreateTransactionWithoutAuth() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT);
        transaction.setName("No Auth");
        transaction.setOriginalAmount(10.00);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testSummaryWithInvalidDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/transactions/summary")
                        .header("Authorization", bearerToken)
                        .param("startDate", "invalid-date")
                        .param("endDate", "2030-12-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testTotalIncomeWithInvalidDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/transactions/total-income")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "31-12-2030"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDateRangeWithInvalidDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020/01/01")
                        .param("endDate", "2030-12-31"))
                .andExpect(status().isBadRequest());
    }


    @Test
    public void testCreateTransactionForForeignAccountUnauthorized() throws Exception {
        // Create another user and their account
        User other = new User();
        other.setName("Other");
        other.setPhoneNo("5550001111");
        other.setEmail("other@example.com");
        other.setPassword("other_pass");
        other = usersRepository.save(other);

        Account foreignAcc = new Account();
        foreignAcc.setName("Foreign Acc");
        foreignAcc.setUserId(other.getId());
        foreignAcc = accountRepository.save(foreignAcc);

        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT);
        transaction.setName("Foreign Post");
        transaction.setOriginalAmount(5.00);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setDate(new Date());
        transaction.setAccountId(foreignAcc.getId());
        transaction.setCategoryId(testCategory.getId());

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetTransactionOfAnotherUserUnauthorized() throws Exception {
        // Another user's transaction
        User other = new User();
        other.setName("Other");
        other.setPhoneNo("5550002222");
        other.setEmail("other2@example.com");
        other.setPassword("other2_pass");
        other = usersRepository.save(other);

        Account otherAcc = new Account();
        otherAcc.setName("Other Acc");
        otherAcc.setUserId(other.getId());
        otherAcc = accountRepository.save(otherAcc);

        Category otherCat = new Category();
        otherCat.setName("OtherCat");
        otherCat.setUserId(other.getId());
        otherCat = categoryRepository.save(otherCat);

        Transaction otherTxn = new Transaction();
        otherTxn.setTransactionType(TransactionDbType.DEBIT);
        otherTxn.setName("OtherTxn");
        otherTxn.setOriginalAmount(3.00);
        otherTxn.setOriginalCurrency("INR");
        otherTxn.setExchangeRate(1.0);
        otherTxn.setDate(new Date());
        otherTxn.setAccountId(otherAcc.getId());
        otherTxn.setCategoryId(otherCat.getId());
        otherTxn = transactionWriteService.saveForUser(other.getId(), otherTxn);

        mockMvc.perform(get("/api/transactions/" + otherTxn.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetMyTransactionsByDateRangeWithAccountFilter() throws Exception {
        Account secondAcc = new Account();
        secondAcc.setName("Spending");
        secondAcc.setUserId(testUser.getId());
        secondAcc = accountRepository.save(secondAcc);

        Transaction t1 = new Transaction();
        t1.setTransactionType(TransactionDbType.DEBIT);
        t1.setName("A1");
        t1.setOriginalAmount(10.00);
        t1.setOriginalCurrency("INR");
        t1.setExchangeRate(1.0);
        t1.setDate(new Date());
        t1.setAccountId(testAccount.getId());
        t1.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), t1);

        Transaction t2 = new Transaction();
        t2.setTransactionType(TransactionDbType.DEBIT);
        t2.setName("A2");
        t2.setOriginalAmount(20.00);
        t2.setOriginalCurrency("INR");
        t2.setExchangeRate(1.0);
        t2.setDate(new Date());
        t2.setAccountId(secondAcc.getId());
        t2.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), t2);

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31")
                        .param("accountIds", String.valueOf(testAccount.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactions", hasSize(1)))
                .andExpect(jsonPath("$.result.transactions[0].name").value("A1"));
    }

    @Test
    public void testGetAccountTransactionsEndpoint_returnsOnlyThatAccount() throws Exception {
        Account secondAcc = new Account();
        secondAcc.setName("Spending");
        secondAcc.setUserId(testUser.getId());
        secondAcc = accountRepository.save(secondAcc);

        Date txDate = new GregorianCalendar(2020, Calendar.JANUARY, 15).getTime();

        Transaction a1 = new Transaction();
        a1.setTransactionType(TransactionDbType.DEBIT);
        a1.setName("A1");
        a1.setOriginalAmount(10.00);
        a1.setOriginalCurrency("INR");
        a1.setExchangeRate(1.0);
        a1.setDate(txDate);
        a1.setAccountId(testAccount.getId());
        a1.setCategoryId(testCategory.getId());
        a1.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), a1);

        Transaction a2 = new Transaction();
        a2.setTransactionType(TransactionDbType.DEBIT);
        a2.setName("A2");
        a2.setOriginalAmount(20.00);
        a2.setOriginalCurrency("INR");
        a2.setExchangeRate(1.0);
        a2.setDate(txDate);
        a2.setAccountId(secondAcc.getId());
        a2.setCategoryId(testCategory.getId());
        a2.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), a2);

        mockMvc.perform(get("/api/accounts/{accountId}/transactions", testAccount.getId())
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2020-02-01")
                        .param("expand", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactions", hasSize(1)))
                .andExpect(jsonPath("$.result.transactions[0].name").value("A1"));
    }

    @Test
    public void testGetAllHidesTransferCreditAndMarksTransferType() throws Exception {
        // Ensure TRANSFER category exists
        Category transfer = new Category();
        transfer.setName("TRANSFER");
        transfer.setUserId(testUser.getId());
        transfer = categoryRepository.save(transfer);

        // Create a transfer pair: debit (type 1, non-countable) and credit (type 2, non-countable)
        Transaction debit = new Transaction();
        debit.setTransactionType(TransactionDbType.DEBIT);
        debit.setName("Transfer Out");
        debit.setOriginalAmount(40.00);
        debit.setOriginalCurrency("INR");
        debit.setExchangeRate(1.0);
        debit.setDate(new Date());
        debit.setAccountId(testAccount.getId());
        debit.setCategoryId(transfer.getId());
        debit.setIsCountable(0);
        transactionWriteService.saveForUser(testUser.getId(), debit);

        Transaction credit = new Transaction();
        credit.setTransactionType(TransactionDbType.CREDIT);
        credit.setName("Transfer In");
        credit.setOriginalAmount(40.00);
        credit.setOriginalCurrency("INR");
        credit.setExchangeRate(1.0);
        credit.setDate(new Date());
        credit.setAccountId(testAccount.getId());
        credit.setCategoryId(transfer.getId());
        credit.setIsCountable(0);
        transactionWriteService.saveForUser(testUser.getId(), credit);

        Calendar now = Calendar.getInstance();
        String month = String.valueOf(now.get(Calendar.MONTH) + 1);
        String year = String.valueOf(now.get(Calendar.YEAR));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", month)
                        .param("year", year))
                .andExpect(status().isOk())
                // CREDIT side should be hidden, only one entry returned
                .andExpect(jsonPath("$.result.transactions", hasSize(1)))
                // transactionType should be marked as 3 (TRANSFER)
                .andExpect(jsonPath("$.result.transactions[0].transactionType").value(3));
    }

    @Test
    public void testGetAllTransactionsPaginatedByMonth() throws Exception {
        Transaction janOlder = new Transaction();
        janOlder.setTransactionType(TransactionDbType.DEBIT);
        janOlder.setName("Jan Old");
        janOlder.setOriginalAmount(15.00);
        janOlder.setOriginalCurrency("INR");
        janOlder.setExchangeRate(1.0);
        janOlder.setDate(new GregorianCalendar(2026, Calendar.JANUARY, 5).getTime());
        janOlder.setAccountId(testAccount.getId());
        janOlder.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), janOlder);

        Transaction janNewer = new Transaction();
        janNewer.setTransactionType(TransactionDbType.DEBIT);
        janNewer.setName("Jan New");
        janNewer.setOriginalAmount(25.00);
        janNewer.setOriginalCurrency("INR");
        janNewer.setExchangeRate(1.0);
        janNewer.setDate(new GregorianCalendar(2026, Calendar.JANUARY, 20).getTime());
        janNewer.setAccountId(testAccount.getId());
        janNewer.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), janNewer);

        Transaction febTransaction = new Transaction();
        febTransaction.setTransactionType(TransactionDbType.DEBIT);
        febTransaction.setName("Feb Tx");
        febTransaction.setOriginalAmount(35.00);
        febTransaction.setOriginalCurrency("INR");
        febTransaction.setExchangeRate(1.0);
        febTransaction.setDate(new GregorianCalendar(2026, Calendar.FEBRUARY, 10).getTime());
        febTransaction.setAccountId(testAccount.getId());
        febTransaction.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), febTransaction);

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", "1")
                        .param("year", "2026")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.month").value(1))
                .andExpect(jsonPath("$.result.year").value(2026))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(1))
                .andExpect(jsonPath("$.result.totalElements").value(2))
                .andExpect(jsonPath("$.result.totalPages").value(2))
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andExpect(jsonPath("$.result.transactions", hasSize(1)))
                .andExpect(jsonPath("$.result.transactions[0].name").value("Jan New"));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", "1")
                        .param("year", "2026")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.page").value(1))
                .andExpect(jsonPath("$.result.hasPrevious").value(true))
                .andExpect(jsonPath("$.result.hasNext").value(false))
                .andExpect(jsonPath("$.result.transactions", hasSize(1)))
                .andExpect(jsonPath("$.result.transactions[0].name").value("Jan Old"));
    }

    @Test
    public void testCurrentUserSummaryIncomeExpenseEndpoints() throws Exception {
        // income
        Transaction income = new Transaction();
        income.setTransactionType(TransactionDbType.CREDIT);
        income.setName("Pay");
        income.setOriginalAmount(120.00);
        income.setOriginalCurrency("INR");
        income.setExchangeRate(1.0);
        income.setDate(new Date());
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        // expense
        Transaction expense = new Transaction();
        expense.setTransactionType(TransactionDbType.DEBIT);
        expense.setName("Snacks");
        expense.setOriginalAmount(20.00);
        expense.setOriginalCurrency("INR");
        expense.setExchangeRate(1.0);
        expense.setDate(new Date());
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(testCategory.getId());
        expense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), expense);

        // /summary
        mockMvc.perform(get("/api/transactions/summary")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalIncome").value(120.00))
                .andExpect(jsonPath("$.result.totalExpense").value(20.00));

        // /total-income
        mockMvc.perform(get("/api/transactions/total-income")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(120.00));

        // /total-expense
        mockMvc.perform(get("/api/transactions/total-expense")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(20.00));
    }

    @Test
    public void testDateRangeParsesMessyAccountIdsGracefully() throws Exception {
        // Add two accounts; filter should only include one valid id among messy input
        Account another = new Account();
        another.setName("Alt");
        another.setUserId(testUser.getId());
        another = accountRepository.save(another);

        Transaction t1 = new Transaction();
        t1.setTransactionType(TransactionDbType.DEBIT);
        t1.setName("KeepMe");
        t1.setOriginalAmount(1.00);
        t1.setOriginalCurrency("INR");
        t1.setExchangeRate(1.0);
        t1.setDate(new Date());
        t1.setAccountId(testAccount.getId());
        t1.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), t1);

        Transaction t2 = new Transaction();
        t2.setTransactionType(TransactionDbType.DEBIT);
        t2.setName("DropMe");
        t2.setOriginalAmount(2.00);
        t2.setOriginalCurrency("INR");
        t2.setExchangeRate(1.0);
        t2.setDate(new Date());
        t2.setAccountId(another.getId());
        t2.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), t2);

        String messy = "  , ,abc,  " + testAccount.getId() + " , x ,";
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31")
                        .param("accountIds", messy))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactions", hasSize(1)))
                .andExpect(jsonPath("$.result.transactions[0].name").value("KeepMe"));
    }

    @Test
    public void testGetAllByDateRange() throws Exception {
        Transaction t = new Transaction();
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setName("DateRangeTx");
        t.setOriginalAmount(50.00);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setDate(new Date());
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), t);

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactions", hasSize(1)))
                .andExpect(jsonPath("$.result.transactions[0].name").value("DateRangeTx"));
    }

    @Test
    public void testGetAllDoesNotReturnOtherUserTransactions() throws Exception {
        // Create another user and transaction
        User other = new User();
        other.setName("Other");
        other.setPhoneNo("5550009999");
        other.setEmail("other99@example.com");
        other.setPassword("other99_pass");
        other = usersRepository.save(other);

        Account otherAcc = new Account();
        otherAcc.setName("Other Acc");
        otherAcc.setUserId(other.getId());
        otherAcc = accountRepository.save(otherAcc);

        Category otherCat = new Category();
        otherCat.setName("Other Cat");
        otherCat.setUserId(other.getId());
        otherCat = categoryRepository.save(otherCat);

        Transaction otherTx = new Transaction();
        otherTx.setTransactionType(TransactionDbType.DEBIT);
        otherTx.setName("Other Tx");
        otherTx.setOriginalAmount(100.00);
        otherTx.setOriginalCurrency("INR");
        otherTx.setExchangeRate(1.0);
        otherTx.setDate(new Date());
        otherTx.setAccountId(otherAcc.getId());
        otherTx.setCategoryId(otherCat.getId());
        transactionWriteService.saveForUser(other.getId(), otherTx);

        // Own transaction
        Transaction myTx = new Transaction();
        myTx.setTransactionType(TransactionDbType.DEBIT);
        myTx.setName("My Tx");
        myTx.setOriginalAmount(50.00);
        myTx.setOriginalCurrency("INR");
        myTx.setExchangeRate(1.0);
        myTx.setDate(new Date());
        myTx.setAccountId(testAccount.getId());
        myTx.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), myTx);

        // Request as testUser
        Calendar now = Calendar.getInstance();
        String month = String.valueOf(now.get(Calendar.MONTH) + 1);
        String year = String.valueOf(now.get(Calendar.YEAR));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", month)
                        .param("year", year))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactions", hasSize(1)))
                .andExpect(jsonPath("$.result.transactions[0].name").value("My Tx"));
    }

    @Test
    public void testGetByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/transactions/999999")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateNotFound() throws Exception {
        Transaction payload = new Transaction();
        payload.setTransactionType(TransactionDbType.DEBIT);
        payload.setName("Missing");
        payload.setOriginalAmount(1.0);
        payload.setOriginalCurrency("INR");
        payload.setExchangeRate(1.0);
        payload.setDate(new Date());
        payload.setAccountId(testAccount.getId());
        payload.setCategoryId(testCategory.getId());

        mockMvc.perform(put("/api/transactions/999999")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteNotFound() throws Exception {
        mockMvc.perform(delete("/api/transactions/999999")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateTransactionWithForeignCategoryUnauthorized() throws Exception {
        // Create foreign category under another user
        User other = new User();
        other.setName("OtherCatUser");
        other.setPhoneNo("5550005555");
        other.setEmail("other5@example.com");
        other.setPassword("other5_pass");
        other = usersRepository.save(other);

        Category foreignCat = new Category();
        foreignCat.setName("ForeignCat");
        foreignCat.setUserId(other.getId());
        foreignCat = categoryRepository.save(foreignCat);

        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionDbType.DEBIT);
        tx.setName("BadCat");
        tx.setOriginalAmount(9.0);
        tx.setOriginalCurrency("INR");
        tx.setExchangeRate(1.0);
        tx.setDate(new Date());
        tx.setAccountId(testAccount.getId());
        tx.setCategoryId(foreignCat.getId());

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateUnauthorizedForForeignAccountAndCategory() throws Exception {
        // Existing txn owned by user
        Transaction existing = new Transaction();
        existing.setTransactionType(TransactionDbType.DEBIT);
        existing.setName("ToUpdate");
        existing.setOriginalAmount(7.0);
        existing.setOriginalCurrency("INR");
        existing.setExchangeRate(1.0);
        existing.setDate(new Date());
        existing.setAccountId(testAccount.getId());
        existing.setCategoryId(testCategory.getId());
        existing = transactionWriteService.saveForUser(testUser.getId(), existing);

        // Create another user and their account/category
        User other = new User();
        other.setName("UpdOther");
        other.setPhoneNo("5550006666");
        other.setEmail("other6@example.com");
        other.setPassword("other6_pass");
        other = usersRepository.save(other);

        Account foreignAcc = new Account();
        foreignAcc.setName("UpdForeignAcc");
        foreignAcc.setUserId(other.getId());
        foreignAcc = accountRepository.save(foreignAcc);

        Category foreignCat = new Category();
        foreignCat.setName("UpdForeignCat");
        foreignCat.setUserId(other.getId());
        foreignCat = categoryRepository.save(foreignCat);

        // Try to update moving to foreign account
        Transaction payload = new Transaction();
        payload.setTransactionType(TransactionDbType.DEBIT);
        payload.setName("ToUpdate2");
        payload.setOriginalAmount(8.0);
        payload.setOriginalCurrency("INR");
        payload.setExchangeRate(1.0);
        payload.setDate(new Date());
        payload.setAccountId(foreignAcc.getId());
        payload.setCategoryId(testCategory.getId());

        mockMvc.perform(put("/api/transactions/" + existing.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());

        // Try to update moving to foreign category (with owned account)
        payload.setAccountId(testAccount.getId());
        payload.setCategoryId(foreignCat.getId());

        mockMvc.perform(put("/api/transactions/" + existing.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeleteUnauthorizedForForeignUser() throws Exception {
        // Create a transaction under another user's account
        User other = new User();
        other.setName("DelOther");
        other.setPhoneNo("5550007777");
        other.setEmail("other7@example.com");
        other.setPassword("other7_pass");
        other = usersRepository.save(other);

        Account otherAcc = new Account();
        otherAcc.setName("DelAcc");
        otherAcc.setUserId(other.getId());
        otherAcc = accountRepository.save(otherAcc);

        Category otherCat = new Category();
        otherCat.setName("DelCat");
        otherCat.setUserId(other.getId());
        otherCat = categoryRepository.save(otherCat);

        Transaction otherTxn = new Transaction();
        otherTxn.setTransactionType(TransactionDbType.DEBIT);
        otherTxn.setName("DelTxn");
        otherTxn.setOriginalAmount(4.0);
        otherTxn.setOriginalCurrency("INR");
        otherTxn.setExchangeRate(1.0);
        otherTxn.setDate(new Date());
        otherTxn.setAccountId(otherAcc.getId());
        otherTxn.setCategoryId(otherCat.getId());
        otherTxn = transactionWriteService.saveForUser(other.getId(), otherTxn);

        mockMvc.perform(delete("/api/transactions/" + otherTxn.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testPageSizeLimit() throws Exception {
        // Test size=10000 (valid)
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31")
                        .param("size", "10000"))
                .andExpect(status().isOk());

        // Test size=10001 (invalid)
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31")
                        .param("size", "10001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size must be between 1 and 10000"));
    }

    @Test
    public void testGetAllRejectsInvalidMonth() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", "13")
                        .param("year", "2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("month must be between 1 and 12"));
    }

    @Test
    public void testGetAllRejectsMissingMonthAndDateRange() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Either month or startDate/endDate must be provided"));
    }

    @Test
    public void testGetAllRejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("page must be 0 or greater"));
    }

    @Test
    public void testGetAllNonExpandWithCategoryIdUsesCategoryBranch() throws Exception {
        Transaction t = new Transaction();
        t.setTransactionType(TransactionDbType.DEBIT);
        t.setName("CatFiltered");
        t.setOriginalAmount(12.34);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setDate(new Date());
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), t);

        Calendar now = Calendar.getInstance();
        String month = String.valueOf(now.get(Calendar.MONTH) + 1);
        String year = String.valueOf(now.get(Calendar.YEAR));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", month)
                        .param("year", year)
                        .param("categoryId", String.valueOf(testCategory.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactions", hasSize(1)))
                .andExpect(jsonPath("$.result.transactions[0].name").value("CatFiltered"));
    }

    @Test
    public void testSummaryIncludeRolloverFalseBranch() throws Exception {
        Transaction income = new Transaction();
        income.setTransactionType(TransactionDbType.CREDIT);
        income.setName("IncomeNoRoll");
        income.setOriginalAmount(100.00);
        income.setOriginalCurrency("INR");
        income.setExchangeRate(1.0);
        income.setDate(new Date());
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        mockMvc.perform(get("/api/transactions/summary")
                        .header("Authorization", bearerToken)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31")
                        .param("includeRollover", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalIncome").value(100.00));
    }

    @Test
    public void testCreateTransferValidationMissingAccountId_triggersBeanValidation() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                null,                    // accountId (missing - should trigger validation)
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                null,                    // originalCurrency
                10.0,                    // originalAmount (using amount field)
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                testAccount.getId(),     // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Failed to create transaction: Transfer requires fromAccountId or accountId"));
    }

    @Test
    public void testCreateTransferMissingToAccountId_noFallback() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                testAccount.getId(),     // accountId
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId (missing for regular transaction validation)
                null,                    // transactionType (missing - should error)
                "INR",                   // originalCurrency
                10.0,                    // originalAmount
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                null,                    // toAccountId (null - should fall back to regular transaction)
                null                     // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("transactionType cannot be null"));
    }

    @Test
    public void testCreateTransferValidationNonPositiveAmount() throws Exception {
        Account toAcc = new Account();
        toAcc.setName("ToAcc");
        toAcc.setUserId(testUser.getId());
        toAcc = accountRepository.save(toAcc);

        TransactionRequest payload = new TransactionRequest(
                null,                    // id
                testAccount.getId(),     // accountId
                new java.util.Date(),    // date
                null,                    // name
                null,                    // comments
                null,                    // categoryId
                TransactionType.TRANSFER,// transactionType
                "USD",                   // originalCurrency
                0.0,                     // originalAmount (invalid - should trigger validation)
                null,                    // exchangeRate
                null,                    // linkedTransactionId
                toAcc.getId(),           // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Failed to create transaction: originalAmount must be greater than 0"));
    }

    @Test
    public void testGetAllExpandTrue_hidesTransferCreditAndMarksTransferTypeForDTO() throws Exception {
        // Ensure the TRANSFER category exists
        Category transfer = new Category();
        transfer.setName("TRANSFER");
        transfer.setUserId(testUser.getId());
        transfer = categoryRepository.save(transfer);

        // Create a transfer pair in the current month for a DTO path
        Transaction debit = new Transaction();
        debit.setTransactionType(TransactionDbType.DEBIT);
        debit.setName("DTO Transfer Out");
        debit.setOriginalAmount(40.00);
        debit.setOriginalCurrency("INR");
        debit.setExchangeRate(1.0);
        debit.setDate(new Date());
        debit.setAccountId(testAccount.getId());
        debit.setCategoryId(transfer.getId());
        debit.setIsCountable(0);
        transactionWriteService.saveForUser(testUser.getId(), debit);

        Transaction credit = new Transaction();
        credit.setTransactionType(TransactionDbType.CREDIT);
        credit.setName("DTO Transfer In");
        credit.setOriginalAmount(40.00);
        credit.setOriginalCurrency("INR");
        credit.setExchangeRate(1.0);
        credit.setDate(new Date());
        credit.setAccountId(testAccount.getId());
        credit.setCategoryId(transfer.getId());
        credit.setIsCountable(0);
        transactionWriteService.saveForUser(testUser.getId(), credit);

        Calendar now = Calendar.getInstance();
        String month = String.valueOf(now.get(Calendar.MONTH) + 1);
        String year = String.valueOf(now.get(Calendar.YEAR));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", month)
                        .param("year", year)
                        .param("expand", "true"))
                .andExpect(status().isOk())
                // credit side hidden, debit side returned
                .andExpect(jsonPath("$.result.transactions", hasSize(1)))
                // marked as type=3 for transfer rendering
                .andExpect(jsonPath("$.result.transactions[0].transactionType").value(3));
    }

    @Test
    public void testPartialUpdateTransaction_OnlyUpdatesProvidedFields() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT);
        transaction.setName("Original Name");
        transaction.setOriginalAmount(10.00);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), transaction);

        // Update only name and amount
        TransactionRequest partialUpdate = new TransactionRequest(
                null,                    // id
                null,                    // accountId (keep existing)
                null,                    // date (keep existing)
                "New Partial Name",     // name
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                "INR",                   // originalCurrency
                20.00,                   // originalAmount
                null,                    // exchangeRate (keep existing)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(put("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("New Partial Name"))
                .andExpect(jsonPath("$.result.amount").value(20.00))
                // Verify other fields remain unchanged (returned in response)
                .andExpect(jsonPath("$.result.accountId").value(testAccount.getId()))
                .andExpect(jsonPath("$.result.categoryId").value(testCategory.getId()));

        // Verify persistence
        Transaction updated = transactionRepository.findById(saved.getId()).orElseThrow();
        assertEquals("New Partial Name", updated.getName());
        assertEquals(20.00, updated.getAmount());
        assertEquals(testAccount.getId(), updated.getAccountId());
    }

    @Test
    public void testPartialUpdateTransfer_UpdatesBothSides() throws Exception {
        Account toAccount = new Account();
        toAccount.setName("Savings 2");
        toAccount.setUserId(testUser.getId());
        toAccount = accountRepository.save(toAccount);

        TransferResult transferPair = transferService.createTransfer(
                testUser.getId(),
                testAccount.getId(),
                toAccount.getId(),
                new Date(),
                50.00,
                "INR",
                1.0,
                "Original Transfer",
                "Original Comment"
        );
        Transaction debit = transferPair.debit();

        // Update name via partial PUT on the debit transaction
        TransactionRequest partialUpdate = new TransactionRequest(
                null,                    // id
                null,                    // accountId (keep existing)
                null,                    // date (keep existing)
                "Updated Transfer Name", // name
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                null,                    // originalCurrency (keep existing)
                null,                    // originalAmount (keep existing)
                null,                    // exchangeRate (keep existing)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(put("/api/transactions/" + debit.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Updated Transfer Name"));

        // Verify both sides updated
        Transaction updatedDebit = transactionRepository.findById(debit.getId()).orElseThrow();
        Transaction updatedCredit = transactionRepository.findById(updatedDebit.getLinkedTransactionId()).orElseThrow();

        assertEquals("Updated Transfer Name", updatedDebit.getName());
        assertEquals("Updated Transfer Name", updatedCredit.getName());
        // Amounts should remain unchanged
        assertEquals(50.00, updatedDebit.getAmount());
        assertEquals(50.00, updatedCredit.getAmount());
    }

    @Test
    public void testPartialUpdateTransaction_ChangeAccountId() throws Exception {
        Account newAccount = new Account();
        newAccount.setName("New Account");
        newAccount.setUserId(testUser.getId());
        newAccount = accountRepository.save(newAccount);

        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT);
        transaction.setName("Move Me");
        transaction.setOriginalAmount(10.00);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), transaction);

        TransactionRequest partialUpdate = new TransactionRequest(
                null,                    // id
                newAccount.getId(),      // accountId (change to new account)
                null,                    // date (keep existing)
                null,                    // name (keep existing)
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                null,                    // originalCurrency (keep existing)
                null,                    // originalAmount (keep existing)
                null,                    // exchangeRate (keep existing)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(put("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accountId").value(newAccount.getId()));

        Transaction updated = transactionRepository.findById(saved.getId()).orElseThrow();
        assertEquals(newAccount.getId(), updated.getAccountId());
    }

    @Test
    public void testUpdateTransaction_RecalculatesAmount_WhenCurrencyFieldsChange() throws Exception {
        // 1. Create a transaction with foreign currency
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT); // Expense
        transaction.setName("Foreign Txn");
        // amount will be calculated: 10 * 1.5 = 15.0
        transaction.setOriginalAmount(10.00);
        transaction.setOriginalCurrency("EUR");
        transaction.setExchangeRate(1.5);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());

        // We let the service calculate the amount on creation
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), transaction);
        assertEquals(15.00, saved.getAmount(), 0.001);

        // 2. Update with new exchange rate (should recalculate amount)
        // New amount should be: 10 * 2.0 = 20.0
        TransactionRequest updatePayload = new TransactionRequest(
                null,                    // id
                null,                    // accountId (keep existing)
                null,                    // date (keep existing)
                null,                    // name (keep existing)
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                null,                    // originalCurrency (keep existing)
                null,                    // originalAmount (keep existing)
                2.0,                     // exchangeRate (change to 2.0)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        // We DO NOT send "amount". We expect the backend to recalculate it because we changed exchangeRate.

        mockMvc.perform(put("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                // Assert that the amount is updated to 20.0
                .andExpect(jsonPath("$.result.amount").value(20.00))
                .andExpect(jsonPath("$.result.exchangeRate").value(2.0));

        // 3. Update with new original amount (should recalculate amount)
        // New amount should be: 20 * 2.0 = 40.0
        TransactionRequest updatePayload2 = new TransactionRequest(
                null,                    // id
                null,                    // accountId (keep existing)
                null,                    // date (keep existing)
                null,                    // name (keep existing)
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                null,                    // originalCurrency (keep existing)
                20.00,                   // originalAmount (change to 20.00)
                null,                    // exchangeRate (keep existing at 2.0)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(put("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload2)))
                .andExpect(status().isOk())
                // Assert that the amount is updated to 40.0
                .andExpect(jsonPath("$.result.amount").value(40.00))
                .andExpect(jsonPath("$.result.originalAmount").value(20.00));
    }

    @Test
    public void testUpdateTransaction_RecalculatesAmount_WhenOriginalCurrencyChanges() throws Exception {
        // 1. Setup: Create a UserCurrency for "GBP" with rate 1.2
        com.trako.entities.UserCurrency userCurrency = new com.trako.entities.UserCurrency();
        userCurrency.setUser(testUser);
        userCurrency.setCurrencyCode("GBP");
        userCurrency.setExchangeRate(1.2);
        userCurrencyRepository.save(userCurrency);

        // 2. Create a transaction with initial currency (e.g. USD default or implicit)
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT);
        transaction.setName("Trip");
        transaction.setOriginalAmount(100.00);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), transaction);

        // 3. Update transaction to use "GBP" and originalAmount 100.
        // Expected amount: 100 * 1.2 = 120.0
        TransactionRequest updatePayload = new TransactionRequest(
                null,                    // id
                null,                    // accountId (keep existing)
                null,                    // date (keep existing)
                null,                    // name (keep existing)
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                "GBP",                   // originalCurrency (change to GBP)
                1.0,                     // originalAmount (change to 1)
                120.0,                   // exchangeRate (explicitly set to 120.0)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );
        // We do NOT send exchangeRate, so it should be looked up from UserCurrency

        mockMvc.perform(put("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.amount").value(120.00))
                .andExpect(jsonPath("$.result.originalCurrency").value("GBP"))
                .andExpect(jsonPath("$.result.exchangeRate").value(120));
    }

    @Test
    public void testUpdateTransaction_DoesNotRecalculate_WhenExplicitAmountProvided() throws Exception {
        // 1. Create a transaction
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT);
        transaction.setName("Explicit Amount Txn");
        transaction.setOriginalAmount(50.00);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), transaction);

        // 2. Update with currency fields BUT also provide explicit amount.
        // If logic was strict: 10 * 2.0 = 20.0.
        // But we send amount = 99.99.
        TransactionRequest updatePayload = new TransactionRequest(
                null,                    // id
                null,                    // accountId (keep existing)
                null,                    // date (keep existing)
                null,                    // name (keep existing)
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                "INR",                   // originalCurrency
                10.00,                   // originalAmount
                null,                    // exchangeRate (keep existing)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(put("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.amount").value(10.00)) // Explicit amount wins
                .andExpect(jsonPath("$.result.originalAmount").value(10.00))
                .andExpect(jsonPath("$.result.exchangeRate").value(1.0));
    }

    @Test
    public void testUpdateTransaction_BaseToSecondary() throws Exception {
        // 1. Setup: Create a UserCurrency for "GBP" with rate 1.2
        com.trako.entities.UserCurrency userCurrency = new com.trako.entities.UserCurrency();
        userCurrency.setUser(testUser);
        userCurrency.setCurrencyCode("GBP");
        userCurrency.setExchangeRate(1.2);
        userCurrencyRepository.save(userCurrency);

        // 2. Create Base Txn (Implicit Base Currency)
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT);
        transaction.setName("Base Txn");
        transaction.setOriginalAmount(100.00);
        transaction.setOriginalCurrency("INR");
        transaction.setExchangeRate(1.0);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), transaction);

        // 3. Update to GBP (Secondary)
        // Should use the stored exchange rate (1.2)
        TransactionRequest updatePayload = new TransactionRequest(
                null,                    // id
                null,                    // accountId (keep existing)
                null,                    // date (keep existing)
                null,                    // name (keep existing)
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                TransactionType.DEBIT,                    // transactionType (keep existing)
                "GBP",                   // originalCurrency (change to GBP)
                1.00,                    // originalAmount (change to 1.00)
                120.0,                   // exchangeRate (set to 120.0)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(put("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.amount").value(120.00))
                .andExpect(jsonPath("$.result.originalCurrency").value("GBP"));
    }

    @Test
    public void testUpdateTransaction_SecondaryToBase() throws Exception {
        // 1. Setup: Create a UserCurrency for "GBP" with rate 1.2
        com.trako.entities.UserCurrency userCurrency = new com.trako.entities.UserCurrency();
        userCurrency.setUser(testUser);
        userCurrency.setCurrencyCode("GBP");
        userCurrency.setExchangeRate(1.2);
        userCurrencyRepository.save(userCurrency);

        // 2. Create Secondary Txn (GBP)
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT);
        transaction.setName("Secondary Txn");
        transaction.setOriginalCurrency("GBP");
        transaction.setOriginalAmount(100.00);
        // Use stored user currency rate 1.2; amount should compute to 120.0
        transaction.setExchangeRate(1.2);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), transaction);
        assertEquals(120.00, saved.getAmount(), 0.001);

        // 3. Update to Base (INR)
        // We provide "INR" as the originalCurrency.
        // The backend should now NORMALIZE to base by setting exchangeRate=1.0 and keeping fields populated.
        TransactionRequest updatePayload = new TransactionRequest(
                null,                    // id
                null,                    // accountId (keep existing)
                null,                    // date (keep existing)
                null,                    // name (keep existing)
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                "INR",                   // originalCurrency (change to INR)
                50.00,                   // originalAmount (change to 50.00)
                1.0,                     // exchangeRate (set to 1.0 for base currency)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(put("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.amount").value(50.00))
                // Expect foreign fields to be POPULATED with Base Context
                .andExpect(jsonPath("$.result.originalCurrency").value("INR"))
                .andExpect(jsonPath("$.result.originalAmount").value(50.00))
                .andExpect(jsonPath("$.result.exchangeRate").value(1.0));
    }

    @Test
    public void testUpdateTransaction_RevertToBaseCurrency_PopulatesBaseFields() throws Exception {
        // 1. Setup: User base currency is INR (default).
        // Create USD currency for user
        com.trako.entities.UserCurrency usd = new com.trako.entities.UserCurrency();
        usd.setUser(testUser);
        usd.setCurrencyCode("USD");
        usd.setExchangeRate(80.0);
        java.util.ArrayList<com.trako.entities.UserCurrency> currs2 = new java.util.ArrayList<>();
        currs2.add(usd);
        testUser.setSecondaryCurrencies(currs2);
        usersRepository.save(testUser);

        // 2. Create Transaction in USD
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionDbType.DEBIT);
        transaction.setName("USD Txn");
        transaction.setOriginalCurrency("USD");
        transaction.setOriginalAmount(10.00);
        transaction.setExchangeRate(80.0);
        // Amount = 800.0
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), transaction);

        assertEquals("USD", saved.getOriginalCurrency());
        assertEquals(800.00, saved.getAmount(), 0.001);

        // 3. Update to INR (Base Currency)
        // We provide originalCurrency="INR" and originalAmount=500.
        // Expectation: Backend detects INR is base currency.
        // Sets amount=500. Populates original* fields with Base context.
        TransactionRequest updatePayload = new TransactionRequest(
                null,                    // id
                null,                    // accountId (keep existing)
                null,                    // date (keep existing)
                null,                    // name (keep existing)
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                "INR",                   // originalCurrency (change to INR)
                500.00,                  // originalAmount (change to 500.00)
                null,                    // exchangeRate (auto-set to 1.0 for base currency)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(put("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.amount").value(500.00))
                .andExpect(jsonPath("$.result.originalCurrency").value("INR"))
                .andExpect(jsonPath("$.result.originalAmount").value(500.00))
                .andExpect(jsonPath("$.result.exchangeRate").value(1.0));

        // Verify DB
        Transaction updated = transactionRepository.findById(saved.getId()).orElseThrow();
        assertEquals(500.00, updated.getAmount(), 0.001);
        assertEquals("INR", updated.getOriginalCurrency());
        assertEquals(500.00, updated.getOriginalAmount(), 0.001);
        assertEquals(1.0, updated.getExchangeRate(), 0.001);
    }

    @Test
    public void testUpdateCurrencyOnly_BaseToSecondary_UsesExistingOriginalAmount() throws Exception {
        // Configure secondary currency GBP at 1.2
        com.trako.entities.UserCurrency gbp = new com.trako.entities.UserCurrency();
        gbp.setUser(testUser);
        gbp.setCurrencyCode("GBP");
        gbp.setExchangeRate(1.2);
        java.util.ArrayList<com.trako.entities.UserCurrency> currs1 = new java.util.ArrayList<>();
        currs1.add(gbp);
        testUser.setSecondaryCurrencies(currs1);
        usersRepository.save(testUser);

        // Create base transaction INR 200 (rate 1.0)
        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionDbType.DEBIT);
        tx.setName("Base 200");
        tx.setOriginalAmount(200.00);
        tx.setOriginalCurrency("INR");
        tx.setExchangeRate(1.0);
        tx.setDate(new Date());
        tx.setAccountId(testAccount.getId());
        tx.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), tx);

        // Update ONLY currency to GBP; expect amount = 200 * 1.2 = 240
        TransactionRequest updatePayload = new TransactionRequest(
                null,                    // id
                null,                    // accountId (keep existing)
                null,                    // date (keep existing)
                null,                    // name (keep existing)
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                "GBP",                   // originalCurrency (change to GBP)
                null,                    // originalAmount (keep existing)
                null,                    // exchangeRate (auto-resolve from UserCurrency: 1.2)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(put("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.amount").value(240.00))
                .andExpect(jsonPath("$.result.originalAmount").value(200.00))
                .andExpect(jsonPath("$.result.originalCurrency").value("GBP"))
                .andExpect(jsonPath("$.result.exchangeRate").value(1.2));
    }

    @Test
    public void testUpdateCurrencyOnly_SecondaryToBase_UsesExistingOriginalAmount() throws Exception {
        // Configure USD as secondary at 80.0
        com.trako.entities.UserCurrency usd = new com.trako.entities.UserCurrency();
        usd.setUser(testUser);
        usd.setCurrencyCode("USD");
        usd.setExchangeRate(80.0);
        userCurrencyRepository.save(usd);

        // Create USD transaction with originalAmount=10 (amount will compute to 800)
        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionDbType.DEBIT);
        tx.setName("USD 10");
        tx.setOriginalCurrency("USD");
        tx.setOriginalAmount(10.00);
        tx.setExchangeRate(80.0);
        tx.setDate(new Date());
        tx.setAccountId(testAccount.getId());
        tx.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), tx);

        // Update ONLY currency to base INR; expect amount = 10 * 1.0 = 10
        TransactionRequest updatePayload = new TransactionRequest(
                null,                    // id
                null,                    // accountId (keep existing)
                null,                    // date (keep existing)
                null,                    // name (keep existing)
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                "INR",                   // originalCurrency (change to INR)
                null,                    // originalAmount (keep existing)
                null,                    // exchangeRate (auto-set to 1.0 for base currency)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(put("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.amount").value(10.00))
                .andExpect(jsonPath("$.result.originalAmount").value(10.00))
                .andExpect(jsonPath("$.result.originalCurrency").value("INR"))
                .andExpect(jsonPath("$.result.exchangeRate").value(1.0));
    }

    @Test
    public void testUpdateCurrencyOnly_SecondaryToSecondary_UsesNewRate() throws Exception {
        // Configure USD=80.0 and GBP=1.2
        com.trako.entities.UserCurrency usd = new com.trako.entities.UserCurrency();
        usd.setUser(testUser);
        usd.setCurrencyCode("USD");
        usd.setExchangeRate(80.0);

        com.trako.entities.UserCurrency gbp = new com.trako.entities.UserCurrency();
        gbp.setUser(testUser);
        gbp.setCurrencyCode("GBP");
        gbp.setExchangeRate(1.2);

        java.util.ArrayList<com.trako.entities.UserCurrency> currs3 = new java.util.ArrayList<>();
        currs3.add(usd);
        currs3.add(gbp);
        testUser.setSecondaryCurrencies(currs3);
        usersRepository.save(testUser);

        // Create USD transaction with originalAmount=1 (amount=80)
        Transaction tx = new Transaction();
        tx.setTransactionType(TransactionDbType.DEBIT);
        tx.setName("USD 1");
        tx.setOriginalCurrency("USD");
        tx.setOriginalAmount(1.00);
        tx.setExchangeRate(80.0);
        tx.setDate(new Date());
        tx.setAccountId(testAccount.getId());
        tx.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), tx);

        // Update ONLY currency to GBP; expect amount = 1 * 1.2 = 1.2
        TransactionRequest updatePayload = new TransactionRequest(
                null,                    // id
                null,                    // accountId (keep existing)
                null,                    // date (keep existing)
                null,                    // name (keep existing)
                null,                    // comments (keep existing)
                null,                    // categoryId (keep existing)
                null,                    // transactionType (keep existing)
                "GBP",                   // originalCurrency (change to GBP)
                null,                    // originalAmount (keep existing)
                null,                    // exchangeRate (auto-resolve from UserCurrency: 1.2)
                null,                    // linkedTransactionId
                null,                    // toAccountId
                null                     // fromAccountId
        );

        mockMvc.perform(put("/api/transactions/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.amount").value(1.20))
                .andExpect(jsonPath("$.result.originalAmount").value(1.00))
                .andExpect(jsonPath("$.result.originalCurrency").value("GBP"))
                .andExpect(jsonPath("$.result.exchangeRate").value(1.2));
    }
}
