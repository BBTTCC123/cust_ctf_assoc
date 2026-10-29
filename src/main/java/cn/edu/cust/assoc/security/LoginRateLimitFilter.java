package cn.edu.cust.assoc.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 登录 POST 在进入用户名密码过滤器前先检查失败次数，避免持续触发 BCrypt。 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginAttemptService attempts;

    public LoginRateLimitFilter(LoginAttemptService attempts) {
        this.attempts = attempts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean loginRequest = "POST".equalsIgnoreCase(request.getMethod())
                && "/login".equals(path);
        String username = loginRequest ? request.getParameter("username") : null;
        String remoteAddress = request.getRemoteAddr();
        if (loginRequest && attempts.isBlocked(username, remoteAddress)) {
            response.sendRedirect(request.getContextPath() + "/login?error=too_many");
            return;
        }
        filterChain.doFilter(request, response);
        if (loginRequest) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof AssocUserDetails) {
                attempts.success(username, remoteAddress);
            } else {
                attempts.failure(username, remoteAddress);
            }
        }
    }
}
