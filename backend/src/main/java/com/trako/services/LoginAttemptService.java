package com.trako.services;

import com.trako.entities.User;
import com.trako.repositories.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Enforces per-account login lockout to defend against password brute-forcing.
 * <p>
 * After {@code security.lockout.max-attempts} consecutive failed attempts an account is
 * locked. The lock auto-expires after a delay that grows exponentially with each repeated
 * lockout cycle ({@code base * 2^(cycle-1)}), clamped at {@code security.lockout.max-lock-seconds}
 * (~1 week by default). Once at the cap the delay stays at the cap rather than growing further.
 * A successful login clears all lockout state.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    @Autowired
    private UsersRepository usersRepository;

    @Value("${security.lockout.enabled:true}")
    private boolean enabled;

    @Value("${security.lockout.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.lockout.base-lock-seconds:60}")
    private long baseLockSeconds;

    @Value("${security.lockout.max-lock-seconds:604800}")
    private long maxLockSeconds;

    /**
     * Returns whether the account is currently locked. If a previous lock has expired it is
     * cleared (and persisted) so the user gets a fresh set of attempts, while the lockout
     * count is retained so continued abuse keeps escalating the backoff.
     */
    public boolean isLocked(User user) {
        if (!enabled || user == null) {
            return false;
        }
        Date lockUntil = user.getLockUntil();
        if (lockUntil == null) {
            return false;
        }
        if (lockUntil.after(new Date())) {
            return true;
        }
        // Lock expired -> clear it, but keep lockoutCount so the backoff keeps escalating.
        user.setLockUntil(null);
        usersRepository.save(user);
        return false;
    }

    /**
     * Seconds remaining until the account unlocks (0 if not currently locked).
     */
    public long retryAfterSeconds(User user) {
        if (user == null || user.getLockUntil() == null) {
            return 0;
        }
        long remainingMs = user.getLockUntil().getTime() - System.currentTimeMillis();
        return remainingMs > 0 ? (remainingMs + 999) / 1000 : 0;
    }

    /**
     * Records a failed login attempt, locking the account (with exponential backoff) once the
     * failure threshold is reached.
     *
     * @return true if this failure just locked the account
     */
    public boolean onFailure(User user) {
        if (!enabled || user == null) {
            return false;
        }
        int attempts = nz(user.getFailedLoginAttempts()) + 1;
        user.setFailedLoginAttempts(attempts);

        boolean justLocked = false;
        if (attempts >= maxAttempts) {
            int lockoutCount = nz(user.getLockoutCount()) + 1;
            long durationSec = lockDurationSeconds(lockoutCount);
            user.setLockoutCount(lockoutCount);
            user.setLockUntil(new Date(System.currentTimeMillis() + durationSec * 1000L));
            user.setFailedLoginAttempts(0);
            justLocked = true;
            log.warn("Account {} locked after {} failed attempts (lockout cycle {}, {}s)",
                    user.getId(), maxAttempts, lockoutCount, durationSec);
        }
        usersRepository.save(user);
        return justLocked;
    }

    /**
     * Clears all lockout state after a successful authentication.
     */
    public void onSuccess(User user) {
        if (user == null) {
            return;
        }
        boolean dirty = nz(user.getFailedLoginAttempts()) != 0
                || nz(user.getLockoutCount()) != 0
                || user.getLockUntil() != null;
        if (!dirty) {
            return;
        }
        user.setFailedLoginAttempts(0);
        user.setLockoutCount(0);
        user.setLockUntil(null);
        usersRepository.save(user);
    }

    /**
     * Exponential backoff: {@code base * 2^(lockoutCount-1)}, computed iteratively so it never
     * overflows and plateaus once it reaches the configured cap.
     */
    private long lockDurationSeconds(int lockoutCount) {
        long duration = baseLockSeconds;
        for (int i = 1; i < lockoutCount && duration < maxLockSeconds; i++) {
            duration = Math.min(duration * 2, maxLockSeconds);
        }
        return Math.min(duration, maxLockSeconds);
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
