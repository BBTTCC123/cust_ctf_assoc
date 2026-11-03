package cn.edu.cust.assoc.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 登录 POST 在进入用户名密码过滤器前先检查失败次数，避免持续触发 BCrypt。 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private final LoginAttemptService attempts;
    private final AuthenticationAuditService audit;

    public LoginRateLimitFilter(LoginAttemptService attempts, AuthenticationAuditService audit) {
        this.attempts = attempts;
        this.audit = audit;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean loginRequest = "POST".equalsIgnoreCase(request.getMethod())
                && "/login".equals(path);
        String username = loginRequest ? request.getParameter("username") : null;
        String remoteAddress = request.getRemoteAddr();
        LoginAttemptService.BlockDecision decision = loginRequest
                ? attempts.check(username, remoteAddress)
                : LoginAttemptService.BlockDecision.none();
        if (decision.blocked()) {
            log.debug("Login blocked reason={} retryAfter={}s ip={} username={}",
                    decision.reason(), decision.retryAfterSeconds(), remoteAddress, safeForLog(username));
            response.sendRedirect(request.getContextPath() + "/login?error=too_many");
            return;
        }
        filterChain.doFilter(request, response);
        if (loginRequest) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof AssocUserDetails) {
                attempts.success(username, remoteAddress);
                audit.success(username);
            } else if (isLoginFailure(response)) {
                LoginAttemptService.FailureResult result = attempts.failure(username, remoteAddress);
                audit.failure(username);
                if (result.cooldownTriggered()) {
                    audit.cooldown(username, LoginAttemptService.USER_IP_COOLDOWN_MS / 1000);
                }
                if (result.ipLimitTriggered()) {
                    audit.ipBlocked(username, LoginAttemptService.IP_WINDOW_MS / 1000);
                }
            }
        }
    }

    private boolean isLoginFailure(HttpServletResponse response) {
        String redirect = response.getHeader("Location");
        return response.getStatus() >= 300 && response.getStatus() < 400
                && redirect != null && redirect.contains("/login?error");
    }

    private String safeForLog(String value) {
        if (value == null) return "";
        String normalized = value.strip().replaceAll("\\p{Cntrl}", " ");
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }
}
