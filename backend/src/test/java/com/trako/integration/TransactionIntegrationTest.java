package com.trako.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.repositories.AccountRepository;
import com.trako.repositories.CategoryRepository;
import com.trako.repositories.TransactionRepository;
import com.trako.repositories.UsersRepository;
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

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionWriteService transactionWriteService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private User testUser;
    private Account testAccount;
    private Category testCategory;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        categoryRepository.deleteAll();
        usersRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhoneNo("1234567890");
        testUser.setEmail("test@example.com");
        testUser.setFireBaseId("password");
        testUser = usersRepository.save(testUser);

        var principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(),
                testUser.getFireBaseId(),
                Collections.emptyList()
        );
        bearerToken = "Bearer " + jwtTokenUtil.generateToken(principal);

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
        transaction.setTransactionType(1);
        transaction.setName("Lunch");
        transaction.setAmount(25.50);
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
    public void testGetAllTransactions() throws Exception {
        Transaction transaction1 = new Transaction();
        transaction1.setTransactionType(1);
        transaction1.setName("Lunch");
        transaction1.setAmount(25.50);
        transaction1.setDate(new Date());
        transaction1.setAccountId(testAccount.getId());
        transaction1.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), transaction1);

        Transaction transaction2 = new Transaction();
        transaction2.setTransactionType(1);
        transaction2.setName("Dinner");
        transaction2.setAmount(35.00);
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
    public void testGetTransactionById() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("Coffee");
        transaction.setAmount(5.00);
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
    public void testGetTransactionsByAccountId() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("ATM Withdrawal");
        transaction.setAmount(200.00);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), transaction);

        mockMvc.perform(get("/api/transactions/account/" + testAccount.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].name").value("ATM Withdrawal"));
    }

    @Test
    public void testGetTransactionsByCategoryId() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("Restaurant");
        transaction.setAmount(50.00);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), transaction);

        mockMvc.perform(get("/api/transactions/category/" + testCategory.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].name").value("Restaurant"));
    }

    @Test
    public void testUpdateTransaction() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("Old Name");
        transaction.setAmount(10.00);
        transaction.setDate(new Date());
        transaction.setAccountId(testAccount.getId());
        transaction.setCategoryId(testCategory.getId());
        Transaction saved = transactionWriteService.saveForUser(testUser.getId(), transaction);

        saved.setName("Updated Name");
        saved.setAmount(15.00);

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
        transaction.setTransactionType(1);
        transaction.setName("To Delete");
        transaction.setAmount(1.00);
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
        income.setTransactionType(2); // CREDIT = income
        income.setName("Salary");
        income.setAmount(1000.00);
        income.setDate(new Date());
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        // Create expense transaction
        Transaction expense = new Transaction();
        expense.setTransactionType(1); // DEBIT = expense
        expense.setName("Groceries");
        expense.setAmount(200.00);
        expense.setDate(new Date());
        expense.setAccountId(testAccount.getId());
        expense.setCategoryId(testCategory.getId());
        expense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), expense);

        // Create non-countable transaction (should be excluded)
        Transaction nonCountable = new Transaction();
        nonCountable.setTransactionType(1); // DEBIT = expense
        nonCountable.setName("Transfer");
        nonCountable.setAmount(50.00);
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
    public void testGetTotalIncome() throws Exception {
        Transaction income1 = new Transaction();
        income1.setTransactionType(2); // CREDIT = income
        income1.setName("Salary");
        income1.setAmount(1000.00);
        income1.setDate(new Date());
        income1.setAccountId(testAccount.getId());
        income1.setCategoryId(testCategory.getId());
        income1.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income1);

        Transaction income2 = new Transaction();
        income2.setTransactionType(2); // CREDIT = income
        income2.setName("Bonus");
        income2.setAmount(500.00);
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
        expense1.setTransactionType(1); // DEBIT = expense
        expense1.setName("Groceries");
        expense1.setAmount(200.00);
        expense1.setDate(new Date());
        expense1.setAccountId(testAccount.getId());
        expense1.setCategoryId(testCategory.getId());
        expense1.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), expense1);

        Transaction expense2 = new Transaction();
        expense2.setTransactionType(1); // DEBIT = expense
        expense2.setName("Utilities");
        expense2.setAmount(150.00);
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
        nonCountable1.setTransactionType(2); // CREDIT = income
        nonCountable1.setName("Transfer In");
        nonCountable1.setAmount(500.00);
        nonCountable1.setDate(new Date());
        nonCountable1.setAccountId(testAccount.getId());
        nonCountable1.setCategoryId(testCategory.getId());
        nonCountable1.setIsCountable(0);
        transactionWriteService.saveForUser(testUser.getId(), nonCountable1);

        Transaction nonCountable2 = new Transaction();
        nonCountable2.setTransactionType(1); // DEBIT = expense
        nonCountable2.setName("Transfer Out");
        nonCountable2.setAmount(300.00);
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
        transaction.setTransactionType(1);
        transaction.setName("No Auth");
        transaction.setAmount(10.00);
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
        other.setFireBaseId("other_pass");
        other = usersRepository.save(other);

        Account foreignAcc = new Account();
        foreignAcc.setName("Foreign Acc");
        foreignAcc.setUserId(other.getId());
        foreignAcc = accountRepository.save(foreignAcc);

        Transaction transaction = new Transaction();
        transaction.setTransactionType(1);
        transaction.setName("Foreign Post");
        transaction.setAmount(5.00);
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
        other.setFireBaseId("other2_pass");
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
        otherTxn.setTransactionType(1);
        otherTxn.setName("OtherTxn");
        otherTxn.setAmount(3.00);
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
        t1.setTransactionType(1);
        t1.setName("A1");
        t1.setAmount(10.00);
        t1.setDate(new Date());
        t1.setAccountId(testAccount.getId());
        t1.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), t1);

        Transaction t2 = new Transaction();
        t2.setTransactionType(1);
        t2.setName("A2");
        t2.setAmount(20.00);
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
    public void testGetAllHidesTransferCreditAndMarksTransferType() throws Exception {
        // Ensure TRANSFER category exists
        Category transfer = new Category();
        transfer.setName("TRANSFER");
        transfer.setUserId(testUser.getId());
        transfer = categoryRepository.save(transfer);

        // Create a transfer pair: debit (type 1, non-countable) and credit (type 2, non-countable)
        Transaction debit = new Transaction();
        debit.setTransactionType(1);
        debit.setName("Transfer Out");
        debit.setAmount(40.00);
        debit.setDate(new Date());
        debit.setAccountId(testAccount.getId());
        debit.setCategoryId(transfer.getId());
        debit.setIsCountable(0);
        transactionWriteService.saveForUser(testUser.getId(), debit);

        Transaction credit = new Transaction();
        credit.setTransactionType(2);
        credit.setName("Transfer In");
        credit.setAmount(40.00);
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
        janOlder.setTransactionType(1);
        janOlder.setName("Jan Old");
        janOlder.setAmount(15.00);
        janOlder.setDate(new GregorianCalendar(2026, Calendar.JANUARY, 5).getTime());
        janOlder.setAccountId(testAccount.getId());
        janOlder.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), janOlder);

        Transaction janNewer = new Transaction();
        janNewer.setTransactionType(1);
        janNewer.setName("Jan New");
        janNewer.setAmount(25.00);
        janNewer.setDate(new GregorianCalendar(2026, Calendar.JANUARY, 20).getTime());
        janNewer.setAccountId(testAccount.getId());
        janNewer.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), janNewer);

        Transaction febTransaction = new Transaction();
        febTransaction.setTransactionType(1);
        febTransaction.setName("Feb Tx");
        febTransaction.setAmount(35.00);
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
        income.setTransactionType(2);
        income.setName("Pay");
        income.setAmount(120.00);
        income.setDate(new Date());
        income.setAccountId(testAccount.getId());
        income.setCategoryId(testCategory.getId());
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        // expense
        Transaction expense = new Transaction();
        expense.setTransactionType(1);
        expense.setName("Snacks");
        expense.setAmount(20.00);
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
    public void testAccountAndCategoryEndpointsWithoutAuthReturnUnauthorized() throws Exception {
        // Create one txn
        Transaction t = new Transaction();
        t.setTransactionType(1);
        t.setName("CatAcc");
        t.setAmount(5.00);
        t.setDate(new Date());
        t.setAccountId(testAccount.getId());
        t.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), t);

        // account/{id} without auth
        mockMvc.perform(get("/api/transactions/account/" + testAccount.getId()))
                .andExpect(status().isUnauthorized());

        // category/{id} without auth
        mockMvc.perform(get("/api/transactions/category/" + testCategory.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testDateRangeParsesMessyAccountIdsGracefully() throws Exception {
        // Add two accounts; filter should only include one valid id among messy input
        Account another = new Account();
        another.setName("Alt");
        another.setUserId(testUser.getId());
        another = accountRepository.save(another);

        Transaction t1 = new Transaction();
        t1.setTransactionType(1);
        t1.setName("KeepMe");
        t1.setAmount(1.00);
        t1.setDate(new Date());
        t1.setAccountId(testAccount.getId());
        t1.setCategoryId(testCategory.getId());
        transactionWriteService.saveForUser(testUser.getId(), t1);

        Transaction t2 = new Transaction();
        t2.setTransactionType(1);
        t2.setName("DropMe");
        t2.setAmount(2.00);
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
        t.setTransactionType(1);
        t.setName("DateRangeTx");
        t.setAmount(50.00);
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
        other.setFireBaseId("other99_pass");
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
        otherTx.setTransactionType(1);
        otherTx.setName("Other Tx");
        otherTx.setAmount(100.00);
        otherTx.setDate(new Date());
        otherTx.setAccountId(otherAcc.getId());
        otherTx.setCategoryId(otherCat.getId());
        transactionWriteService.saveForUser(other.getId(), otherTx);

        // Own transaction
        Transaction myTx = new Transaction();
        myTx.setTransactionType(1);
        myTx.setName("My Tx");
        myTx.setAmount(50.00);
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
        payload.setTransactionType(1);
        payload.setName("Missing");
        payload.setAmount(1.0);
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
        other.setFireBaseId("other5_pass");
        other = usersRepository.save(other);

        Category foreignCat = new Category();
        foreignCat.setName("ForeignCat");
        foreignCat.setUserId(other.getId());
        foreignCat = categoryRepository.save(foreignCat);

        Transaction tx = new Transaction();
        tx.setTransactionType(1);
        tx.setName("BadCat");
        tx.setAmount(9.0);
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
        existing.setTransactionType(1);
        existing.setName("ToUpdate");
        existing.setAmount(7.0);
        existing.setDate(new Date());
        existing.setAccountId(testAccount.getId());
        existing.setCategoryId(testCategory.getId());
        existing = transactionWriteService.saveForUser(testUser.getId(), existing);

        // Create another user and their account/category
        User other = new User();
        other.setName("UpdOther");
        other.setPhoneNo("5550006666");
        other.setEmail("other6@example.com");
        other.setFireBaseId("other6_pass");
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
        payload.setTransactionType(1);
        payload.setName("ToUpdate2");
        payload.setAmount(8.0);
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
        other.setFireBaseId("other7_pass");
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
        otherTxn.setTransactionType(1);
        otherTxn.setName("DelTxn");
        otherTxn.setAmount(4.0);
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
}
