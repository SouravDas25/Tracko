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
 * Property-based test for Cross-Field Search Completeness.
 * Validates: Requirements 1.2, 2.1, 2.2
 */
@Transactional
public class SearchCrossFieldPropertyTest extends BaseIntegrationTest {

    private static final Random RANDOM = new Random();

    private String token;
    private Account account;
    private Category category;

    @BeforeEach
    public void setup() {
        User user = createUniqueUser("CrossField Property User");
        token = generateBearerToken(user);

        account = new Account();
        account.setName("CrossField Account");
        account.setUserId(user.getId());
        account.setCurrency("INR");
        account = accountRepository.save(account);

        category = new Category();
        category.setName("CrossField Category");
        category.setUserId(user.getId());
        category.setCategoryType(CategoryType.EXPENSE);
        category = categoryRepository.save(category);
    }

    @RepeatedTest(5)
    public void transactionWithSearchTermInNameIsFound() throws Exception {
        String term = generateUniqueTerm();
        Transaction tx = createTx(term + " expense item", "unrelated comment", 50.0);

        String response = searchRaw(term);
        List<Integer> ids = JsonPath.read(response, "$.result.results[*].transaction.id");
        Integer totalResults = JsonPath.read(response, "$.result.totalResults");
        assertTrue(totalResults >= 1, "At least 1 result expected for name match");
        assertTrue(ids.contains(tx.getId().intValue()),
                "Transaction with term '" + term + "' in NAME should be found");
    }

    @RepeatedTest(5)
    public void transactionWithSearchTermInCommentsIsFound() throws Exception {
        String term = generateUniqueTerm();
        Transaction tx = createTx("generic payment", "note about " + term + " purchase", 75.0);

        String response = searchRaw(term);
        List<Integer> ids = JsonPath.read(response, "$.result.results[*].transaction.id");
        Integer totalResults = JsonPath.read(response, "$.result.totalResults");
        assertTrue(totalResults >= 1, "At least 1 result expected for comments match");
        assertTrue(ids.contains(tx.getId().intValue()),
                "Transaction with term '" + term + "' in COMMENTS should be found");
    }

    @RepeatedTest(5)
    public void bothNameAndCommentsMatchesAreReturned() throws Exception {
        String term = generateUniqueTerm();
        Transaction nameTx = createTx(term + " store visit", "paid with card", 30.0);
        Transaction commentsTx = createTx("regular purchase", "related to " + term + " order", 60.0);

        String response = searchRaw(term);
        List<Integer> ids = JsonPath.read(response, "$.result.results[*].transaction.id");
        Integer totalResults = JsonPath.read(response, "$.result.totalResults");
        assertTrue(totalResults >= 2, "At least 2 results expected when term in name and comments");
        assertTrue(ids.contains(nameTx.getId().intValue()), "Name-match should be in results");
        assertTrue(ids.contains(commentsTx.getId().intValue()), "Comments-match should be in results");
    }

    @RepeatedTest(3)
    public void transactionWithTermInBothFieldsReportsMatchedFields() throws Exception {
        String term = generateUniqueTerm();
        Transaction tx = createTx(term + " dinner", "enjoyed " + term + " restaurant", 45.0);

        String response = mockMvc.perform(get("/api/transactions/search")
                        .param("query", term).param("size", "100").param("expand", "true")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Integer> ids = JsonPath.read(response, "$.result.results[*].transaction.id");
        assertTrue(ids.contains(tx.getId().intValue()));

        // Find the hit and verify matchedFields
        List<Map<String, Object>> results = JsonPath.read(response, "$.result.results[*]");
        for (Map<String, Object> hit : results) {
            Map<String, Object> transaction = (Map<String, Object>) hit.get("transaction");
            if (Objects.equals(transaction.get("id"), tx.getId().intValue())) {
                List<String> matchedFields = (List<String>) hit.get("matchedFields");
                assertNotNull(matchedFields);
                assertTrue(matchedFields.contains("name"));
                assertTrue(matchedFields.contains("comments"));
            }
        }
    }

    @RepeatedTest(3)
    public void transactionWithoutSearchTermIsNotReturned() throws Exception {
        String term = generateUniqueTerm();
        Transaction unrelated = createTx("completely different name", "no matching content", 100.0);
        Transaction matching = createTx(term + " item", "some comment", 25.0);

        List<Integer> ids = searchAndGetIds(term);
        assertTrue(ids.contains(matching.getId().intValue()));
        assertFalse(ids.contains(unrelated.getId().intValue()),
                "Transaction without term should NOT be returned");
    }

    private List<Integer> searchAndGetIds(String query) throws Exception {
        String response = searchRaw(query);
        return JsonPath.read(response, "$.result.results[*].transaction.id");
    }

    private String searchRaw(String query) throws Exception {
        return mockMvc.perform(get("/api/transactions/search")
                        .param("query", query).param("size", "100")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String generateUniqueTerm() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder("xfld");
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        return sb.toString();
    }

    private Transaction createTx(String name, String comments, double amount) {
        Transaction t = new Transaction();
        t.setName(name);
        t.setComments(comments);
        t.setDate(new Date());
        t.setOriginalAmount(amount);
        t.setOriginalCurrency("INR");
        t.setExchangeRate(1.0);
        t.setAccountId(account.getId());
        t.setCategoryId(category.getId());
        t.setTransactionType(TransactionDbType.DEBIT);
        return transactionRepository.save(t);
    }
}
