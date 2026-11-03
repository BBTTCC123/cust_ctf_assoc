package cn.edu.cust.assoc.security;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    @Test
    void pairCooldownLastsOneMinuteAndDoesNotBecomeAnAccountWideLock() {
        AtomicLong now = new AtomicLong(1_000);
        LoginAttemptService service = new LoginAttemptService(now::get);

        for (int i = 0; i < LoginAttemptService.USER_IP_LIMIT; i++) {
            service.failure("Yorkchain", "198.51.100.10");
        }

        assertThat(service.check("Yorkchain", "198.51.100.10").reason())
                .isEqualTo(LoginAttemptService.BlockReason.USER_COOLDOWN);
        assertThat(service.check("Yorkchain", "198.51.100.11").blocked()).isFalse();

        now.addAndGet(LoginAttemptService.USER_IP_COOLDOWN_MS + 1);
        assertThat(service.check("Yorkchain", "198.51.100.10").blocked()).isFalse();
    }

    @Test
    void ipWideLimitStopsUsernameSpraying() {
        AtomicLong now = new AtomicLong(1_000);
        LoginAttemptService service = new LoginAttemptService(now::get);

        for (int i = 0; i < LoginAttemptService.IP_LIMIT; i++) {
            service.failure("user" + i, "203.0.113.20");
        }

        assertThat(service.check("another-user", "203.0.113.20").reason())
                .isEqualTo(LoginAttemptService.BlockReason.IP_RATE_LIMIT);

        now.addAndGet(LoginAttemptService.IP_WINDOW_MS + 1);
        assertThat(service.check("another-user", "203.0.113.20").blocked()).isFalse();
    }
}
