package cn.edu.cust.assoc.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 首次登录 / 管理员重置后的强制改密闸门。
 * 除改密页、登录退出和静态资源外，所有请求都跳到 /password/change。
 */
public class MustChangePasswordFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof AssocUserDetails details
                && details.isMustChangePassword()
                && !isAllowed(request.getRequestURI())) {
            response.sendRedirect(request.getContextPath() + "/password/change");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String path) {
        return path.equals("/password/change")
                || path.equals("/logout")
                || path.equals("/login")
                || path.equals("/error")
                || path.equals("/favicon.ico")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/");
    }
}
