package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.security.AssocUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

/**
 * 会话撤销。禁用账号、改角色、重置密码后调用：目标用户的全部会话立即过期，
 * 下一次请求会被 Spring Security 的并发会话过滤器踢回登录页。
 */
@Service
public class SessionRevocationService {

    private static final Logger log = LoggerFactory.getLogger(SessionRevocationService.class);

    private final SessionRegistry sessionRegistry;

    public SessionRevocationService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public int expireAll(Long userId) {
        int expired = 0;
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof AssocUserDetails details && details.getId().equals(userId)) {
                for (SessionInformation info : sessionRegistry.getAllSessions(principal, false)) {
                    info.expireNow();
                    expired++;
                }
            }
        }
        if (expired > 0) {
            log.info("已撤销用户 {} 的 {} 个会话", userId, expired);
        }
        return expired;
    }
}
