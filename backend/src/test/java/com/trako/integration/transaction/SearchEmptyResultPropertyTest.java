package com.trako.integration.transaction;

import com.trako.entities.Account;
import com.trako.entities.User;
import com.trako.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based test for Empty Result Handling.
 * Validates: Requirements 1.5
 */
@Transactional
public class SearchEmptyResultPropertyTest extends BaseIntegrationTest {

    private static final Random RANDOM = new Random();
    private String token;

    @BeforeEach
    public void setup() {
        User user = createUniqueUser("EmptyResult Property User");
        token = generateBearerToken(user);

        Account account = new Account();
        account.setName("EmptyResult Account");
        account.setUserId(user.getId());
        account.setCurrency("INR");
        accountRepository.save(account);
    }

    @RepeatedTest(5)
    public void randomQueryReturnsZeroTotalResults() throws Exception {
        mockMvc.perform(get("/api/transactions/search")
                        .param("query", generateRandomQuery())
                        .param("page", "0").param("size", "20")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalResults").value(0));
    }

    @RepeatedTest(5)
    public void randomQueryReturnsEmptyResultsList() throws Exception {
        mockMvc.perform(get("/api/transactions/search")
                        .param("query", generateRandomQuery())
                        .param("page", "0").param("size", "20")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.results").isEmpty());
    }

    @RepeatedTest(5)
    public void randomQueryReturnsCorrectMetadata() throws Exception {
        mockMvc.perform(get("/api/transactions/search")
                        .param("query", generateRandomQuery())
                        .param("page", "0").param("size", "20")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.hasNext").value(false))
                .andExpect(jsonPath("$.result.hasPrevious").value(false))
                .andExpect(jsonPath("$.result.searchTimeMs").isNumber());
    }

    @RepeatedTest(5)
    public void resultQueryFieldMatchesInputQuery() throws Exception {
        String query = generateRandomQuery();
        mockMvc.perform(get("/api/transactions/search")
                        .param("query", query)
                        .param("page", "0").param("size", "20")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.query").value(query));
    }

    private String generateRandomQuery() {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder("zempty");
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        return sb.toString();
    }
}
