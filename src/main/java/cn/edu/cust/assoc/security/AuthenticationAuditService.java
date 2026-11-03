package cn.edu.cust.assoc.security;

import cn.edu.cust.assoc.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 认证审计写入失败不能阻断登录、登出主流程。 */
@Service
public class AuthenticationAuditService {

    public static final String LOGIN_SUCCESS = "AUTH_LOGIN_SUCCESS";
    public static final String LOGIN_FAILURE = "AUTH_LOGIN_FAILURE";
    public static final String LOGIN_COOLDOWN = "AUTH_LOGIN_COOLDOWN";
    public static final String LOGIN_BLOCKED = "AUTH_LOGIN_BLOCKED";
    public static final String LOGOUT = "AUTH_LOGOUT";

    private static final Logger log = LoggerFactory.getLogger(AuthenticationAuditService.class);

    private final AuditService audit;

    public AuthenticationAuditService(AuditService audit) {
        this.audit = audit;
    }

    public void success(String username) {
        safely(() -> audit.record(LOGIN_SUCCESS, "AUTH", null, null, "登录成功"));
    }

    public void failure(String username) {
        safely(() -> audit.recordAuthenticationEvent(LOGIN_FAILURE, username, "账号或密码不正确"));
    }

    public void cooldown(String username, long retryAfterSeconds) {
        safely(() -> audit.recordAuthenticationEvent(LOGIN_COOLDOWN, username,
                "同一来源连续失败，冷却 " + retryAfterSeconds + " 秒"));
    }

    public void ipBlocked(String username, long retryAfterSeconds) {
        safely(() -> audit.recordAuthenticationEvent(LOGIN_BLOCKED, username,
                "来源 IP 触发登录限流，剩余 " + retryAfterSeconds + " 秒"));
    }

    public void logout(String username) {
        safely(() -> audit.recordAuthenticationEvent(LOGOUT, username, "主动退出"));
    }

    private void safely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.error("认证审计写入失败", e);
        }
    }
}
