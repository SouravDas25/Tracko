package com.trako.integration.transaction;

import com.trako.config.TestJwtSecurityConfig;
import com.trako.entities.User;
import com.trako.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtSecurityConfig.class)
@Transactional
public class TransactionGetAllMalformedAccountIdsTest extends BaseIntegrationTest {

    private String bearerToken;

    @BeforeEach
    public void setup() {
        User u = createUniqueUser("U1");
        bearerToken = generateBearerToken(u);
    }

    @Test
    public void getAll_malformedAccountIds_parsesGracefully_returnsOk() throws Exception {
        Calendar cal = Calendar.getInstance();
        int m = cal.get(Calendar.MONTH) + 1;
        int y = cal.get(Calendar.YEAR);
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", bearerToken)
                        .param("month", String.valueOf(m))
                        .param("year", String.valueOf(y))
                        .param("accountIds", "abc,123, ,xyz"))
                .andExpect(status().isOk());
    }
}
