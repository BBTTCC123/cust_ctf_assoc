package cn.edu.cust.assoc.security;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 登录失败限流。
 *
 * 策略分成两层：
 * - 同一 IP + 登录名失败 5 次，只冷却 60 秒，避免长期锁死唯一超管；
 * - 同一来源 IP 在 15 分钟内累计失败 30 次，阻断该 IP 的后续登录尝试。
 *
 * 单机部署使用进程内计数；多实例部署时应替换为 Redis。
 */
@Service
public class LoginAttemptService {

    static final int USER_IP_LIMIT = 5;
    static final long USER_IP_COOLDOWN_MS = 60_000L;
    static final int IP_LIMIT = 30;
    static final long IP_WINDOW_MS = 15 * 60_000L;

    public enum BlockReason {
        NONE,
        USER_COOLDOWN,
        IP_RATE_LIMIT
    }

    public record BlockDecision(BlockReason reason, long retryAfterSeconds) {
        public boolean blocked() {
            return reason != BlockReason.NONE;
        }

        static BlockDecision none() {
            return new BlockDecision(BlockReason.NONE, 0);
        }
    }

    public record FailureResult(boolean cooldownTriggered, boolean ipLimitTriggered) {
    }

    private static final class Counter {
        private long windowStart;
        private long count;
        private long blockedUntil;

        private Counter(long windowStart) {
            this.windowStart = windowStart;
        }
    }

    private final Map<String, Counter> pairFailures = new ConcurrentHashMap<>();
    private final Map<String, Counter> ipFailures = new ConcurrentHashMap<>();
    private final LongSupplier nowMillis;

    public LoginAttemptService() {
        this(System::currentTimeMillis);
    }

    LoginAttemptService(LongSupplier nowMillis) {
        this.nowMillis = nowMillis;
    }

    public BlockDecision check(String username, String remoteAddress) {
        long now = nowMillis.getAsLong();
        String ip = normalizeIp(remoteAddress);

        Counter ipCounter = ipFailures.get(ip);
        if (ipCounter != null) {
            synchronized (ipCounter) {
                if (now - ipCounter.windowStart >= IP_WINDOW_MS) {
                    ipFailures.remove(ip, ipCounter);
                } else if (ipCounter.count >= IP_LIMIT) {
                    long seconds = Math.max(1, (IP_WINDOW_MS - (now - ipCounter.windowStart) + 999) / 1000);
                    return new BlockDecision(BlockReason.IP_RATE_LIMIT, seconds);
                }
            }
        }

        String pairKey = pairKey(username, ip);
        Counter pairCounter = pairFailures.get(pairKey);
        if (pairCounter != null) {
            synchronized (pairCounter) {
                if (pairCounter.blockedUntil > now) {
                    long seconds = Math.max(1, (pairCounter.blockedUntil - now + 999) / 1000);
                    return new BlockDecision(BlockReason.USER_COOLDOWN, seconds);
                }
                if (now - pairCounter.windowStart >= USER_IP_COOLDOWN_MS) {
                    pairFailures.remove(pairKey, pairCounter);
                }
            }
        }

        return BlockDecision.none();
    }

    /** 兼容旧调用方；新代码应使用 {@link #check(String, String)} 获取阻断原因。 */
    public boolean isBlocked(String username, String remoteAddress) {
        return check(username, remoteAddress).blocked();
    }

    public FailureResult failure(String username, String remoteAddress) {
        long now = nowMillis.getAsLong();
        String ip = normalizeIp(remoteAddress);
        String pairKey = pairKey(username, ip);

        Counter ipCounter = ipFailures.compute(ip, (ignored, old) ->
                old == null || now - old.windowStart >= IP_WINDOW_MS
                        ? new Counter(now)
                        : old);
        boolean ipLimitTriggered;
        synchronized (ipCounter) {
            ipCounter.count++;
            ipLimitTriggered = ipCounter.count == IP_LIMIT;
        }

        Counter pairCounter = pairFailures.compute(pairKey, (ignored, old) ->
                old == null || now - old.windowStart >= USER_IP_COOLDOWN_MS
                        ? new Counter(now)
                        : old);
        boolean cooldownTriggered;
        synchronized (pairCounter) {
            pairCounter.count++;
            cooldownTriggered = pairCounter.count == USER_IP_LIMIT;
            if (cooldownTriggered) {
                pairCounter.windowStart = now;
                pairCounter.blockedUntil = now + USER_IP_COOLDOWN_MS;
            }
        }

        cleanup(now);
        return new FailureResult(cooldownTriggered, ipLimitTriggered);
    }

    public void success(String username, String remoteAddress) {
        pairFailures.remove(pairKey(username, normalizeIp(remoteAddress)));
    }

    private void cleanup(long now) {
        if (pairFailures.size() > 10_000) {
            pairFailures.entrySet().removeIf(e ->
                    now - e.getValue().windowStart >= USER_IP_COOLDOWN_MS
                            && e.getValue().blockedUntil <= now);
        }
        if (ipFailures.size() > 10_000) {
            ipFailures.entrySet().removeIf(e -> now - e.getValue().windowStart >= IP_WINDOW_MS);
        }
    }

    private String pairKey(String username, String ip) {
        String normalizedUsername = username == null
                ? ""
                : username.strip().toLowerCase(Locale.ROOT);
        return ip + '|' + normalizedUsername;
    }

    private String normalizeIp(String remoteAddress) {
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress.strip();
    }
}
