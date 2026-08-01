package com.trako.integration.currency;

import com.trako.entities.Account;
import com.trako.entities.Category;
import com.trako.entities.User;
import com.trako.entities.UserCurrency;
import com.trako.enums.TransactionType;
import com.trako.integration.BaseIntegrationTest;
import com.trako.models.external.ExchangeRateApiResponse;
import com.trako.models.request.TransactionRequest;
import com.trako.repositories.UserCurrencyRepository;
import com.trako.services.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the direction of the exchange rate resolved from the live rate provider.
 *
 * <p>The provider ({@code open.er-api.com/v6/latest/{base}}) quotes rates as
 * <em>units of the target per 1 unit of base</em> — so {@code getRates("INR").rates["EUR"]}
 * answers "how many EUR is 1 INR worth".
 *
 * <p>The schema needs the opposite. {@code transactions.amount} is a generated column
 * defined as {@code original_amount * exchange_rate}, and {@code amount} is denominated in
 * the user's base currency — so {@code exchange_rate} must be <em>base units per 1 unit of
 * the foreign currency</em>. The provider's quote has to be inverted before it is used or stored.
 *
 * <p>These tests mock the provider in its real direction (unlike the other currency tests,
 * which mock it already inverted and therefore cannot catch this).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ExchangeRateDirectionIntegrationTest extends BaseIntegrationTest {

    /** Provider quote for base INR: 1 INR buys 0.01 EUR. */
    private static final double INR_TO_EUR_QUOTE = 0.01;

    /** What the app must store and apply: 1 EUR is worth 100 INR. */
    private static final double EXPECTED_EUR_TO_INR_RATE = 1.0 / INR_TO_EUR_QUOTE;

    @Autowired
    private UserCurrencyRepository userCurrencyRepository;

    @MockBean
    private ExchangeRateService exchangeRateService;

    private User testUser;
    private String bearerToken;
    private Account aib;
    private Account hdfc;
    private Category category;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("Direction User");
        testUser.setBaseCurrency("INR");
        testUser = usersRepository.save(testUser);

        bearerToken = generateBearerToken(testUser);

        aib = new Account();
        aib.setName("AIB");
        aib.setUserId(testUser.getId());
        aib.setCurrency("EUR");
        aib = accountRepository.save(aib);

        hdfc = new Account();
        hdfc.setName("HDFC");
        hdfc.setUserId(testUser.getId());
        hdfc.setCurrency("INR");
        hdfc = accountRepository.save(hdfc);

        category = new Category();
        category.setName("Remittance");
        category.setUserId(testUser.getId());
        category = categoryRepository.save(category);

        // Mocked in the provider's own direction: base INR, 1 INR = 0.01 EUR.
        ExchangeRateApiResponse liveRates =
                new ExchangeRateApiResponse("INR", Map.of("EUR", INR_TO_EUR_QUOTE));
        when(exchangeRateService.getRates(anyString())).thenReturn(liveRates);
    }

    @Test
    public void regularTransaction_invertsProviderQuoteBeforeApplyingIt() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                     // id
                aib.getId(),              // accountId
                new Date(),               // date
                "Dinner in Dublin",       // name
                null,                     // comments
                category.getId(),         // categoryId
                TransactionType.DEBIT,    // transactionType
                "EUR",                    // originalCurrency
                100.0,                    // originalAmount
                null,                     // exchangeRate — forces live resolution
                null,                     // linkedTransactionId
                null,                     // toAccountId
                null                      // fromAccountId
        );

        // 100 EUR at 100 INR/EUR = 10,000 INR.
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.exchangeRate").value(closeTo(EXPECTED_EUR_TO_INR_RATE, 1e-9)))
                .andExpect(jsonPath("$.result.amount").value(closeTo(10_000.0, 1e-6)));
    }

    @Test
    public void transfer_invertsProviderQuoteBeforeApplyingIt() throws Exception {
        // Mirrors: trako transaction add-transfer --amount 3000 --currency EUR
        //          --from-account-name "AIB" --to-account-name "HDFC" --name "Instarem"
        // The CLI has no --exchange-rate flag, so exchangeRate always arrives null here.
        TransactionRequest payload = new TransactionRequest(
                null,                     // id
                aib.getId(),              // accountId (source)
                new Date(),               // date
                "Instarem",               // name
                null,                     // comments
                null,                     // categoryId
                TransactionType.TRANSFER, // transactionType
                "EUR",                    // originalCurrency
                3000.0,                   // originalAmount
                null,                     // exchangeRate — forces live resolution
                null,                     // linkedTransactionId
                hdfc.getId(),             // toAccountId
                null                      // fromAccountId
        );

        // 3000 EUR at 100 INR/EUR = 300,000 INR.
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.exchangeRate").value(closeTo(EXPECTED_EUR_TO_INR_RATE, 1e-9)))
                .andExpect(jsonPath("$.result.amount").value(closeTo(300_000.0, 1e-6)));
    }

    @Test
    public void liveResolution_storesRateInBaseCurrencyDirection() throws Exception {
        TransactionRequest payload = new TransactionRequest(
                null,                     // id
                aib.getId(),              // accountId
                new Date(),               // date
                "Groceries",              // name
                null,                     // comments
                category.getId(),         // categoryId
                TransactionType.DEBIT,    // transactionType
                "EUR",                    // originalCurrency
                50.0,                     // originalAmount
                null,                     // exchangeRate — forces live resolution
                null,                     // linkedTransactionId
                null,                     // toAccountId
                null                      // fromAccountId
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        // The row cached as a side effect of resolution must be in the same direction
        // a user would enter by hand via `trako currency update --code EUR --rate ...`.
        UserCurrency stored =
                userCurrencyRepository.findByUserIdAndCurrencyCode(testUser.getId(), "EUR");
        assertNotNull(stored, "live resolution should have cached a EUR rate");
        assertEquals(EXPECTED_EUR_TO_INR_RATE, stored.getExchangeRate(), 1e-9,
                "stored rate must be INR-per-EUR, not the provider's EUR-per-INR quote");
    }
}
