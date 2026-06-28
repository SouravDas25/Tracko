package com.trako.integration.session;

import com.trako.entities.User;
import com.trako.integration.BaseIntegrationTest;
import com.trako.models.request.LoginRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the lockout can be fully disabled via configuration: failed attempts never lock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = "security.lockout.enabled=false")
public class LoginLockoutDisabledTest extends BaseIntegrationTest {

    private User testUser;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("Disabled Lockout User");
    }

    @Test
    public void failedAttemptsNeverLockWhenDisabled() throws Exception {
        String username = testUser.getPhoneNo();
        for (int i = 0; i < 8; i++) {
            login(username, "wrong").andExpect(status().isUnauthorized());
        }

        // No lock state should have been recorded
        User reloaded = usersRepository.findByPhoneNo(username);
        Assertions.assertEquals(0, reloaded.getFailedLoginAttempts());
        Assertions.assertNull(reloaded.getLockUntil());

        // And correct credentials still work
        login(username, TEST_PASSWORD).andExpect(status().isOk());
    }

    private ResultActions login(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }
}
