package cn.edu.cust.assoc.security;

import cn.edu.cust.assoc.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 公开端点的安全头与招新限流（ISS-001 / ISS-009）。
 *
 * CSP 是阻止 Markdown 注入后二次执行脚本的最后一道闸；限流按真实 IP 与学号
 * 各 5 次/小时，MockMvc 里通过设置 remoteAddr 模拟不同来源。
 */
@Transactional
@EnabledIf("cn.edu.cust.assoc.support.TestDatabase#isAvailable")
class PublicEndpointIT extends IntegrationTestBase {

    private static final AtomicInteger IP_SEQ = new AtomicInteger();
    private static final AtomicInteger STUDENT_SEQ = new AtomicInteger();

    @Autowired
    private LoginAttemptService loginAttempts;

    @Test
    void securityHeadersAreSentOnPublicPages() throws Exception {
        for (String url : new String[]{"/", "/login", "/register", "/join"}) {
            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Security-Policy",
                            containsString("default-src 'self'")))
                    .andExpect(header().string("Content-Security-Policy",
                            containsString("script-src 'self'")))
                    .andExpect(header().string("Content-Security-Policy",
                            containsString("frame-ancestors 'none'")))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Referrer-Policy", "same-origin"));
        }
    }

    @Test
    void joinIsLimitedToFiveSubmissionsPerIpPerHour() throws Exception {
        String ip = "198.51.100." + IP_SEQ.incrementAndGet();

        for (int i = 0; i < 5; i++) {
            String studentNo = nextStudentNo();
            mockMvc.perform(post("/join").with(csrf()).with(remoteAddr(ip))
                            .param("name", "限流测试")
                            .param("studentNo", studentNo)
                            .param("college", "计算机学院")
                            .param("major", "网络空间安全")
                            .param("contact", "13800000000"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/join"))
                    .andExpect(flash().attribute("message", "报名已提交，我们会联系你"));
        }

        // 第 6 次即使换了学号，同一 IP 也会被拦住
        mockMvc.perform(post("/join").with(csrf()).with(remoteAddr(ip))
                        .param("name", "限流测试")
                        .param("studentNo", nextStudentNo())
                        .param("college", "计算机学院")
                        .param("major", "网络空间安全")
                        .param("contact", "13800000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/join"))
                .andExpect(flash().attribute("errorMessage", "提交太频繁，过一会儿再试"));
    }

    @Test
    void repeatedLoginFailuresBlockFurtherPasswordChecks() throws Exception {
        String username = createActiveUser("loginlimit", "LoginLimit!2026abc").getUsername();
        String ip = "203.0.113." + IP_SEQ.incrementAndGet();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login").with(csrf()).with(remoteAddr(ip))
                            .param("username", username)
                            .param("password", "WrongLimit!2026"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }

        org.assertj.core.api.Assertions.assertThat(loginAttempts.isBlocked(username, ip)).isTrue();

        mockMvc.perform(post("/login").with(csrf()).with(remoteAddr(ip))
                        .param("username", username)
                        .param("password", "LoginLimit!2026abc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=too_many"));
    }

    private String nextStudentNo() {
        return String.format("%012d", 202600000000L + STUDENT_SEQ.incrementAndGet());
    }

    private RequestPostProcessor remoteAddr(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
