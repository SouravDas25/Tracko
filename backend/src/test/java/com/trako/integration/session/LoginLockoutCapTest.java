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

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the exponential backoff clamps at {@code max-lock-seconds} and plateaus there.
 * Config: lock after 2 attempts, base 10s, cap 25s → cycle durations 10s, 20s, 25s, 25s.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "security.lockout.max-attempts=2",
        "security.lockout.base-lock-seconds=10",
        "security.lockout.max-lock-seconds=25"
})
public class LoginLockoutCapTest extends BaseIntegrationTest {

    private static final long CAP_MS = 25_000L;
    private static final long TOLERANCE_MS = 2_000L;

    private User testUser;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("Cap Lockout User");
    }

    @Test
    public void lockDurationPlateausAtCap() throws Exception {
        String username = testUser.getPhoneNo();

        long d1 = triggerLockAndMeasure(username); // ~10s
        expireLock(username);
        long d2 = triggerLockAndMeasure(username); // ~20s
        expireLock(username);
        long d3 = triggerLockAndMeasure(username); // capped ~25s
        expireLock(username);
        long d4 = triggerLockAndMeasure(username); // capped ~25s

        Assertions.assertTrue(d2 > d1, "expected growth before the cap: d2=" + d2 + " d1=" + d1);
        Assertions.assertTrue(Math.abs(d3 - CAP_MS) <= TOLERANCE_MS, "d3 should be ~cap: " + d3);
        Assertions.assertTrue(Math.abs(d4 - CAP_MS) <= TOLERANCE_MS, "d4 should be ~cap: " + d4);
        Assertions.assertTrue(Math.abs(d4 - d3) <= TOLERANCE_MS, "duration should plateau: d3=" + d3 + " d4=" + d4);
    }

    /** With max-attempts=2: one 401 then the locking attempt. Returns lock duration in ms. */
    private long triggerLockAndMeasure(String username) throws Exception {
        login(username, "wrong").andExpect(status().isUnauthorized());
        long before = System.currentTimeMillis();
        login(username, "wrong").andExpect(status().isTooManyRequests());
        User locked = usersRepository.findByPhoneNo(username);
        Assertions.assertNotNull(locked.getLockUntil());
        return locked.getLockUntil().getTime() - before;
    }

    private void expireLock(String username) {
        User user = usersRepository.findByPhoneNo(username);
        user.setLockUntil(new Date(System.currentTimeMillis() - 1000));
        usersRepository.save(user);
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
