package cn.edu.cust.assoc.web;

import cn.edu.cust.assoc.dto.AbilityRadar;
import cn.edu.cust.assoc.dto.MemberLedgerRow;
import cn.edu.cust.assoc.dto.MemberPointsView;
import cn.edu.cust.assoc.dto.RadarAxis;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 页面级测试：真实渲染 member.html，确认六维雷达与公开数据出现在最终 HTML 里。 */
class MemberPageTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("成员公开页渲染出 SVG 雷达图、六类标签与积分")
    void rendersRadarChart() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        AbilityRadar radar = AbilityRadar.of(List.of(
                new RadarAxis("Web", 2, 1), new RadarAxis("Pwn", 1, 0),
                new RadarAxis("Reverse", 0, 0), new RadarAxis("Crypto", 0, 0),
                new RadarAxis("Misc", 0, 0), new RadarAxis("应急响应", 0, 1)));
        MemberPointsView view = new MemberPointsView(7L, "阿蓝", 42,
                Map.of("比赛奖", 42),
                List.of(new MemberLedgerRow("2026-10", LocalDate.of(2026, 10, 20),
                        "比赛奖", "EARN", 42)),
                null, radar);

        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application =
                JakartaServletWebApplication.buildApplication(servletContext);
        WebContext context = new WebContext(application.buildExchange(
                new MockHttpServletRequest(), new MockHttpServletResponse()),
                Locale.SIMPLIFIED_CHINESE);
        context.setVariable("member", view);
        String html = engine().process("member", context);

        assertThat(html).contains("阿蓝");
        assertThat(html).contains("能力雷达（六类竞赛）");
        assertThat(html).contains("<svg");
        assertThat(html).contains("radar-personal");
        assertThat(html).contains("radar-team");
        assertThat(html).contains(">Web</text>");
        assertThat(html).contains(">应急响应</text>");
        assertThat(html).contains("42");
    }

    private SpringTemplateEngine engine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
