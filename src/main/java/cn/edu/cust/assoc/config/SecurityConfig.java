package cn.edu.cust.assoc.config;

import cn.edu.cust.assoc.security.DbUserDetailsService;
import cn.edu.cust.assoc.security.MustChangePasswordFilter;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
public class SecurityConfig {

    /**
     * 不依赖 CDN、不跑内联脚本；样式里还有少量 style 属性，所以放开 style 的内联。
     * 注意：模板里不要再写 onclick 之类的内联事件，统一走 /js/app.js。
     */
    private static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "font-src 'self'",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /** 让 SessionRegistry 能收到会话销毁事件，否则禁用/降权后列表会越积越多。 */
    @Bean
    public static ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DbUserDetailsService userDetailsService)
            throws Exception {
        http
            .userDetailsService(userDetailsService)
            .authorizeHttpRequests(auth -> auth
                // 公开页面只放行 GET；评论、收藏等写操作必须登录
                .requestMatchers(HttpMethod.GET, "/", "/about", "/competitions", "/competitions/**",
                                "/points", "/members/**", "/articles", "/articles/**",
                                "/announcements", "/announcements/**", "/honors", "/join", "/register",
                                "/login", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/join", "/register").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                // 媒体是否放行由 MediaController 自己判（已审核+公开的才给匿名看），这里必须先放进来
                .requestMatchers("/media/**").permitAll()
                // 健康检查给发布脚本用；生产上 actuator 只绑本机
                .requestMatchers("/actuator/health").permitAll()
                // 账号域只有超管
                .requestMatchers("/admin/users/**", "/admin/audit/**").hasRole("SUPER_ADMIN")
                // 战队管理只有超管；战队空间本身对所有登录成员开放（默认 anyRequest 已拦住匿名）
                .requestMatchers("/admin/teams/**").hasRole("SUPER_ADMIN")
                // 积分域：积分管理员 + 超管
                .requestMatchers("/admin/points/**", "/admin/appeals/**", "/admin/competitions/**",
                                "/admin/rewards/**")
                    .hasAnyRole("SUPER_ADMIN", "POINTS_ADMIN")
                // 内容域：内容管理员 + 超管
                .requestMatchers("/admin/articles/**", "/admin/media/**", "/admin/comments/**", "/admin/sensitive-words/**")
                    .hasAnyRole("SUPER_ADMIN", "CONTENT_ADMIN")
                // 宣传域：宣传管理员 + 超管
                .requestMatchers("/admin/public/**").hasAnyRole("SUPER_ADMIN", "PR_ADMIN")
                .requestMatchers("/admin", "/admin/**")
                    .hasAnyRole("SUPER_ADMIN", "CONTENT_ADMIN", "POINTS_ADMIN", "PR_ADMIN")
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                .referrerPolicy(referrer -> referrer.policy(
                        org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .frameOptions(frame -> frame.deny())
            )
            .sessionManagement(session -> session
                // -1 = 不限制并发数，但要登记每个会话，禁用/降权时好统一踢下线
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry()))
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", false)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            // 登录成功还没改密的人，除了改密页哪都去不了
            .addFilterAfter(new MustChangePasswordFilter(), AuthorizationFilter.class);
        return http.build();
    }
}
