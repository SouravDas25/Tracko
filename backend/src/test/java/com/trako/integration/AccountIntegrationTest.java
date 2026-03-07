package com.trako.integration;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.*;
import com.trako.models.request.AccountSaveRequest;
import com.trako.repositories.RecurringTransactionRepository;
import com.trako.services.transactions.TransactionWriteService;
import com.trako.services.transactions.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class AccountIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TransactionWriteService transactionWriteService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    private User testUser;
    private String bearerToken;

    @BeforeEach
    public void setup() {
        // We don't necessarily need to deleteAll if every test uses a unique user,
        // but it doesn't hurt for small suites. However, for parallel execution,
        // relying on unique users is key.

        testUser = createUniqueUser();
        bearerToken = generateBearerToken(testUser);
    }

    @Test
    public void testGetAccountById_ReturnsHybridBalance_SummaryPlusTransfers() throws Exception {
        // Create accounts
        Account a1 = new Account();
        a1.setName("A1");
        a1.setUserId(testUser.getId());
        a1 = accountRepository.save(a1);

        Account a2 = new Account();
        a2.setName("A2");
        a2.setUserId(testUser.getId());
        a2 = accountRepository.save(a2);

        // Create categories for countable transactions
        Category salary = new Category();
        salary.setName("Salary");
        salary.setUserId(testUser.getId());
        salary = categoryRepository.save(salary);

        Category food = new Category();
        food.setName("Food");
        food.setUserId(testUser.getId());
        food = categoryRepository.save(food);

        // Two different months for summary aggregation
        Date jan10 = new GregorianCalendar(2024, Calendar.JANUARY, 10).getTime();
        Date feb05 = new GregorianCalendar(2024, Calendar.FEBRUARY, 5).getTime();

        // Countable income + expense on A1 (should go into account_month_summary)
        Transaction income = new Transaction();
        income.setTransactionType(TransactionEntryType.CREDIT);
        income.setName("Income");
        income.setOriginalAmount(1000.0);
        income.setOriginalCurrency("INR");
        income.setExchangeRate(1.0);
        income.setDate(jan10);
        income.setAccountId(a1.getId());
        income.setCategoryId(salary.getId());
        income.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), income);

        Transaction expense = new Transaction();
        expense.setTransactionType(TransactionEntryType.DEBIT);
        expense.setName("Expense");
        expense.setOriginalAmount(200.0);
        expense.setOriginalCurrency("INR");
        expense.setExchangeRate(1.0);
        expense.setDate(feb05);
        expense.setAccountId(a1.getId());
        expense.setCategoryId(food.getId());
        expense.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), expense);

        // Transfers (isCountable=0) should NOT affect summary, but MUST affect balance via linkedTransactionId
        transferService.createTransfer(
                testUser.getId(),
                a1.getId(),
                a2.getId(),
                feb05,
                50.0,
                "INR",
                1.0,
                "T1",
                ""
        );

        transferService.createTransfer(
                testUser.getId(),
                a2.getId(),
                a1.getId(),
                feb05,
                30.0,
                "INR",
                1.0,
                "T2",
                ""
        );

        // Expected balance for A1 (derived from transactions only):
        // +1000 (income) - 200 (expense) - 50 (transfer out) + 30 (transfer in) = 780
        mockMvc.perform(get("/api/accounts/" + a1.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(a1.getId()))
                .andExpect(jsonPath("$.result.balance").value(780.0));

        // Expected balance for A2 (derived from transactions only):
        // +50 (transfer in) - 30 (transfer out) = 20
        mockMvc.perform(get("/api/accounts/" + a2.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(a2.getId()))
                .andExpect(jsonPath("$.result.balance").value(20.0));
    }

    @Test
    public void testCreateAccountWithoutNameReturnsBadRequest() throws Exception {
        AccountSaveRequest req = new AccountSaveRequest();
        // name is null -> @NotNull should trigger 400

        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateAccountWithoutNameReturnsBadRequest() throws Exception {
        Account saved = new Account();
        saved.setName("HasName");
        saved.setUserId(testUser.getId());
        saved = accountRepository.save(saved);

        AccountSaveRequest req = new AccountSaveRequest();
        // name not set -> should 400

        mockMvc.perform(put("/api/accounts/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateAccountWithoutAuthReturnsUnauthorized() throws Exception {
        AccountSaveRequest req = new AccountSaveRequest();
        req.setName("NoAuth");

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetAllAccounts_doesNotReturnOtherUsersAccounts() throws Exception {
        User other = new User();
        other.setName("OtherU");
        other.setPhoneNo("2002002000");
        other.setEmail("otheru@example.com");
        other.setPassword("other_pass");
        other = usersRepository.save(other);

        Account mine = new Account();
        mine.setName("Mine");
        mine.setUserId(testUser.getId());
        accountRepository.save(mine);

        Account foreign = new Account();
        foreign.setName("Foreign");
        foreign.setUserId(other.getId());
        accountRepository.save(foreign);

        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].name").value("Mine"));
    }

    @Test
    public void testGetAccountByIdUnauthorizedForForeignUser() throws Exception {
        User other = new User();
        other.setName("OtherAcc");
        other.setPhoneNo("3003003000");
        other.setEmail("otheracc@example.com");
        other.setPassword("otheracc_pass");
        other = usersRepository.save(other);

        Account foreign = new Account();
        foreign.setName("Foreign");
        foreign.setUserId(other.getId());
        foreign = accountRepository.save(foreign);

        mockMvc.perform(get("/api/accounts/" + foreign.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateAccountUnauthorizedForForeignUser() throws Exception {
        User other = new User();
        other.setName("UpdOtherAcc");
        other.setPhoneNo("4004004000");
        other.setEmail("updother@example.com");
        other.setPassword("updother_pass");
        other = usersRepository.save(other);

        Account foreign = new Account();
        foreign.setName("ForeignUpd");
        foreign.setUserId(other.getId());
        foreign = accountRepository.save(foreign);

        AccountSaveRequest req = new AccountSaveRequest();
        req.setName("TryUpdate");

        mockMvc.perform(put("/api/accounts/" + foreign.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeleteAccountUnauthorizedForForeignUser() throws Exception {
        User other = new User();
        other.setName("DelOtherAcc");
        other.setPhoneNo("5005005000");
        other.setEmail("delother@example.com");
        other.setPassword("delother_pass");
        other = usersRepository.save(other);

        Account foreign = new Account();
        foreign.setName("ForeignDel");
        foreign.setUserId(other.getId());
        foreign = accountRepository.save(foreign);

        mockMvc.perform(delete("/api/accounts/" + foreign.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetAccountByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/accounts/999999")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteAccount_Fails_WhenTransactionsExist() throws Exception {
        // Create a category and a transaction under the account
        Account a = new Account();
        a.setName("A");
        a.setUserId(testUser.getId());
        a = accountRepository.save(a);

        Category cat = new Category();
        cat.setName("TestCat");
        cat.setUserId(testUser.getId());
        cat = categoryRepository.save(cat);

        Transaction txn = new Transaction();
        txn.setTransactionType(TransactionEntryType.DEBIT);
        txn.setName("Tx");
        txn.setOriginalAmount(10.0);
        txn.setOriginalCurrency("INR");
        txn.setExchangeRate(1.0);
        txn.setDate(new Date());
        txn.setAccountId(a.getId());
        txn.setCategoryId(cat.getId());
        txn.setIsCountable(1);
        transactionWriteService.saveForUser(testUser.getId(), txn);

        mockMvc.perform(delete("/api/accounts/" + a.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot delete account")));
    }

    @Test
    public void testDeleteAccount_Fails_WhenRecurringReferencesExist() throws Exception {
        // Create accounts
        Account a1 = new Account();
        a1.setName("A1");
        a1.setUserId(testUser.getId());
        a1 = accountRepository.save(a1);

        Account a2 = new Account();
        a2.setName("A2");
        a2.setUserId(testUser.getId());
        a2 = accountRepository.save(a2);

        // Minimal category
        Category cat = new Category();
        cat.setName("RecurringCat");
        cat.setUserId(testUser.getId());
        cat = categoryRepository.save(cat);

        // Create a recurring transaction referencing the account to be deleted
        com.trako.entities.RecurringTransaction rt = new com.trako.entities.RecurringTransaction();
        rt.setUserId(testUser.getId());
        rt.setName("R1");
        rt.setOriginalAmount(100.0);
        rt.setOriginalCurrency("INR");
        rt.setExchangeRate(1.0);
        rt.setAccountId(a1.getId());
        rt.setToAccountId(a2.getId());
        rt.setCategoryId(cat.getId());
        rt.setTransactionType(com.trako.entities.TransactionType.DEBIT);
        rt.setFrequency(com.trako.entities.Frequency.MONTHLY);
        rt.setStartDate(new Date());
        rt.setNextRunDate(new Date());
        rt.setIsActive(true);
        recurringTransactionRepository.save(rt);

        mockMvc.perform(delete("/api/accounts/" + a1.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Recurring transactions reference this account")));
    }

    @Test
    public void testUpdateAccountNotFound() throws Exception {
        AccountSaveRequest req = new AccountSaveRequest();
        req.setName("UpdateMissing");

        mockMvc.perform(put("/api/accounts/999999")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateAccount() throws Exception {
        Account account = new Account();
        account.setName("Savings");
        account.setUserId(testUser.getId());

        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(account)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Savings"))
                .andExpect(jsonPath("$.result.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.result.id").isNotEmpty());
    }

    @Test
    public void testGetAllAccounts() throws Exception {
        Account account1 = new Account();
        account1.setName("Savings");
        account1.setUserId(testUser.getId());
        accountRepository.save(account1);

        Account account2 = new Account();
        account2.setName("Cash");
        account2.setUserId(testUser.getId());
        accountRepository.save(account2);

        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[*].name", containsInAnyOrder("Savings", "Cash")));
    }

    @Test
    public void testGetAccountById() throws Exception {
        Account account = new Account();
        account.setName("Checking");
        account.setUserId(testUser.getId());
        Account saved = accountRepository.save(account);

        mockMvc.perform(get("/api/accounts/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Checking"))
                .andExpect(jsonPath("$.result.id").value(saved.getId()));
    }

    @Test
    public void testGetAllAccounts_isScopedToAuthenticatedUser() throws Exception {
        Account account1 = new Account();
        account1.setName("Savings");
        account1.setUserId(testUser.getId());
        accountRepository.save(account1);

        Account account2 = new Account();
        account2.setName("Investment");
        account2.setUserId(testUser.getId());
        accountRepository.save(account2);

        mockMvc.perform(get("/api/accounts")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[*].userId", everyItem(is(testUser.getId()))));
    }

    @Test
    public void testUpdateAccount() throws Exception {
        Account account = new Account();
        account.setName("Old Name");
        account.setUserId(testUser.getId());
        Account saved = accountRepository.save(account);

        saved.setName("New Name");

        mockMvc.perform(put("/api/accounts/" + saved.getId())
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("New Name"))
                .andExpect(jsonPath("$.result.id").value(saved.getId()));
    }

    @Test
    public void testDeleteAccount() throws Exception {
        Account account = new Account();
        account.setName("To Delete");
        account.setUserId(testUser.getId());
        Account saved = accountRepository.save(account);

        mockMvc.perform(delete("/api/accounts/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts/" + saved.getId())
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }
}
