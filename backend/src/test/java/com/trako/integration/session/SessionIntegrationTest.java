package com.trako.integration.session;

import com.trako.entities.User;
import com.trako.integration.BaseIntegrationTest;
import com.trako.models.request.AuthicationRequest;
import com.trako.models.request.LoginRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SessionIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private AuthenticationManager authenticationManager;

    private User testUser;

    @BeforeEach
    public void setup() {
        testUser = createUniqueUser("Test User");

        // For /api/oauth/token we mock the authentication manager so we don't depend on password encoding config.
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                testUser.getPhoneNo(),
                testUser.getPassword(),
                Collections.emptyList()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
    }

    // ---------------------------------------------------------------------------------------------
    // Happy paths
    // ---------------------------------------------------------------------------------------------

    @Test
    public void loginSuccessReturnsJwtToken() throws Exception {
        login(testUser.getPhoneNo(), TEST_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    public void loginWrongPasswordReturnsUnauthorized() throws Exception {
        login(testUser.getPhoneNo(), "wrong")
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void oauthTokenReturnsJwtToken() throws Exception {
        oauth(testUser.getPhoneNo(), testUser.getPassword())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    public void unknownUserReturnsUnauthorized() throws Exception {
        login("0000000000", "whatever")
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------------------------------
    // Lockout behaviour (default config: 5 attempts, 60s base)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void loginLocksAfterFiveFailedAttempts() throws Exception {
        String username = testUser.getPhoneNo();
        for (int i = 0; i < 4; i++) {
            login(username, "wrong").andExpect(status().isUnauthorized());
        }
        // 5th failure trips the lock
        login(username, "wrong")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    public void lockedAccountRejectsEvenCorrectPassword() throws Exception {
        String username = testUser.getPhoneNo();
        for (int i = 0; i < 5; i++) {
            login(username, "wrong");
        }
        // Correct credentials are still refused while locked
        login(username, TEST_PASSWORD)
                .andExpect(status().isTooManyRequests());
    }

    @Test
    public void successfulLoginResetsFailedCounter() throws Exception {
        String username = testUser.getPhoneNo();
        for (int i = 0; i < 4; i++) {
            login(username, "wrong").andExpect(status().isUnauthorized());
        }
        login(username, TEST_PASSWORD).andExpect(status().isOk());

        User reloaded = usersRepository.findByPhoneNo(username);
        Assertions.assertEquals(0, reloaded.getFailedLoginAttempts());
        Assertions.assertEquals(0, reloaded.getLockoutCount());
        Assertions.assertNull(reloaded.getLockUntil());
    }

    @Test
    public void lockExpiryAllowsLoginAgainAndClearsState() throws Exception {
        String username = testUser.getPhoneNo();
        for (int i = 0; i < 5; i++) {
            login(username, "wrong");
        }
        login(username, TEST_PASSWORD).andExpect(status().isTooManyRequests());

        // Simulate the lock window elapsing
        expireLock(username);

        login(username, TEST_PASSWORD).andExpect(status().isOk());

        User reloaded = usersRepository.findByPhoneNo(username);
        Assertions.assertNull(reloaded.getLockUntil());
        Assertions.assertEquals(0, reloaded.getLockoutCount());
        Assertions.assertEquals(0, reloaded.getFailedLoginAttempts());
    }

    @Test
    public void lockDurationGrowsExponentiallyAcrossCycles() throws Exception {
        String username = testUser.getPhoneNo();

        long duration1 = triggerLockAndMeasure(username);
        expireLock(username);
        long duration2 = triggerLockAndMeasure(username);

        // Second cycle (~120s) should be roughly double the first (~60s)
        Assertions.assertTrue(duration2 > duration1,
                "expected duration2 (" + duration2 + ") > duration1 (" + duration1 + ")");
        Assertions.assertTrue(duration2 > duration1 * 1.5,
                "expected exponential growth: duration2 (" + duration2 + ") > 1.5x duration1 (" + duration1 + ")");
    }

    // ---------------------------------------------------------------------------------------------
    // OAuth endpoint lockout + 401 (not 500) on bad credentials
    // ---------------------------------------------------------------------------------------------

    @Test
    public void oauthBadCredentialsReturnsUnauthorizedThenLocks() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        String phone = testUser.getPhoneNo();
        // Single bad-credential attempt must be 401, not 500
        oauth(phone, "wrong").andExpect(status().isUnauthorized());

        for (int i = 0; i < 3; i++) {
            oauth(phone, "wrong").andExpect(status().isUnauthorized());
        }
        // 5th failure trips the lock
        oauth(phone, "wrong")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private ResultActions login(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    private ResultActions oauth(String phoneNo, String password) throws Exception {
        AuthicationRequest req = new AuthicationRequest();
        req.setPhoneNo(phoneNo);
        req.setPassword(password);
        return mockMvc.perform(post("/api/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    /** Fails until just before the lock, then triggers it and returns the lock duration in ms. */
    private long triggerLockAndMeasure(String username) throws Exception {
        for (int i = 0; i < 4; i++) {
            login(username, "wrong").andExpect(status().isUnauthorized());
        }
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
}
