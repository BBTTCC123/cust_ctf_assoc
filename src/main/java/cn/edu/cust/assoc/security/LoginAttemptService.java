package cn.edu.cust.assoc.security;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败限流。
 *
 * 单机部署使用进程内计数：同一 IP + 登录名在 15 分钟内连续失败 5 次后，
 * 后续登录请求在进入 BCrypt 校验前直接拒绝。多实例部署时应替换为 Redis。
 */
@Service
public class LoginAttemptService {

    private static final int LIMIT = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L;

    private final Map<String, long[]> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String username, String remoteAddress) {
        String key = key(username, remoteAddress);
        long now = System.currentTimeMillis();
        long[] slot = attempts.get(key);
        if (slot == null) {
            return false;
        }
        synchronized (slot) {
            if (now - slot[0] > WINDOW_MS) {
                attempts.remove(key, slot);
                return false;
            }
            return slot[1] >= LIMIT;
        }
    }

    public void failure(String username, String remoteAddress) {
        String key = key(username, remoteAddress);
        long now = System.currentTimeMillis();
        long[] slot = attempts.compute(key, (ignored, old) ->
                old == null || now - old[0] > WINDOW_MS ? new long[]{now, 0} : old);
        synchronized (slot) {
            slot[1]++;
        }
        if (attempts.size() > 10_000) {
            attempts.entrySet().removeIf(e -> now - e.getValue()[0] > WINDOW_MS);
        }
    }

    public void success(String username, String remoteAddress) {
        attempts.remove(key(username, remoteAddress));
    }

    private String key(String username, String remoteAddress) {
        return remoteAddress + ":" + (username == null ? "" : username.strip().toLowerCase(Locale.ROOT));
    }
}
