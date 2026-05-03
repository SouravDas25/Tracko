package com.trako.integration.transaction;

import com.jayway.jsonpath.JsonPath;
import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.Transaction;
import com.trako.entities.User;
import com.trako.enums.CategoryType;
import com.trako.enums.TransactionDbType;
import com.trako.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based test for Index Freshness (New Transaction Searchability).
 * Validates: Requirements 5.3
 */
@Transactional
public class SearchIndexFreshnessPropertyTest extends BaseIntegrationTest {

    private static final Random RANDOM = new Random();

    private String token;
    private Account account;
    private Category category;

    @BeforeEach
    public void setup() {
        User user = createUniqueUser("Index Freshness Property User");
        token = generateBearerToken(user);

        account = new Account();
        account.setName("Freshness Test Account");
        account.setUserId(user.getId());
        account.setCurrency("INR");
        account = accountRepository.save(account);

        category = new Category();
        category.setName("Freshness Category");
        category.setUserId(user.getId());
        category.setCategoryType(CategoryType.EXPENSE);
        category = categoryRepository.save(category);
    }

    @RepeatedTest(5)
    public void newlyCreatedTransactionIsImmediatelySearchable() throws Exception {
        String uniqueName = "freshness_" + UUID.randomUUID().toString().substring(0, 12);
        Transaction saved = createTx(uniqueName, "Freshness test comment", 50.0);

        List<Integer> ids = searchAndGetIds(uniqueName);
        assertFalse(ids.isEmpty(),
                "Newly created transaction '" + uniqueName + "' should be immediately searchable");
        assertTrue(ids.contains(saved.getId().intValue()));
    }

    @RepeatedTest(3)
    public void multipleNewTransactionsAreAllImmediatelySearchable() throws Exception {
        String sharedPrefix = "batchfresh_" + UUID.randomUUID().toString().substring(0, 8);
        int count = 3 + RANDOM.nextInt(3);

        Set<Integer> createdIds = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            Transaction saved = createTx(sharedPrefix + " item " + (i + 1), "comment " + i, 25.0);
            createdIds.add(saved.getId().intValue());
        }
        transactionRepository.flush();

        List<Integer> ids = searchAndGetIds(sharedPrefix);
        assertEquals(createdIds.size(), ids.size(),
                "All " + createdIds.size() + " new transactions should be immediately searchable");
        assertTrue(ids.containsAll(createdIds));
    }

    @RepeatedTest(3)
    public void newTransactionIsSearchableByComments() throws Exception {
        String uniqueComment = "commentfresh_" + UUID.randomUUID().toString().substring(0, 12);
        Transaction saved = createTx("Generic Name", uniqueComment, 30.0);

        List<Integer> ids = searchAndGetIds(uniqueComment);
        assertFalse(ids.isEmpty(),
                "Transaction should be searchable by comments field '" + uniqueComment + "'");
        assertTrue(ids.contains(saved.getId().intValue()));
    }

    private List<Integer> searchAndGetIds(String query) throws Exception {
        String response = mockMvc.perform(get("/api/transactions/search")
                        .param("query", query).param("size", "100")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.result.results[*].transaction.id");
    }

    private Transaction createTx(String name, String comments, double amount) {
        Transaction t = new Transaction();
        t.setName(name);
        t.setComments(comments);
        t.setDate(randomDate());
        t.setOriginalAmount(amount);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setAccountId(account.getId());
        t.setCategoryId(category.getId());
        t.setTransactionType(TransactionDbType.DEBIT);
        return transactionRepository.save(t);
    }

    private Date randomDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, RANDOM.nextInt(12), 1 + RANDOM.nextInt(28), 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
