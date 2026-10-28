package cn.edu.cust.assoc.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlPolicyTest {

    @Test
    @DisplayName("危险协议一律拒绝")
    void dangerousSchemes() {
        assertThat(UrlPolicy.sanitizeLink("javascript:alert(1)")).isEmpty();
        assertThat(UrlPolicy.sanitizeLink("JavaScript:alert(1)")).isEmpty();
        assertThat(UrlPolicy.sanitizeLink("java\nscript:alert(1)")).isEmpty();
        assertThat(UrlPolicy.sanitizeLink("  javascript:alert(1)")).isEmpty();
        assertThat(UrlPolicy.sanitizeLink("data:text/html,<script>alert(1)</script>")).isEmpty();
        assertThat(UrlPolicy.sanitizeLink("vbscript:msgbox(1)")).isEmpty();
        assertThat(UrlPolicy.sanitizeImage("data:image/png;base64,AAAA")).isEmpty();
    }

    @Test
    @DisplayName("白名单协议与站内相对链接放行")
    void allowed() {
        assertThat(UrlPolicy.sanitizeLink("https://example.com/a?b=1")).isEqualTo("https://example.com/a?b=1");
        assertThat(UrlPolicy.sanitizeLink("http://example.com")).isEqualTo("http://example.com");
        assertThat(UrlPolicy.sanitizeLink("mailto:a@example.com")).isEqualTo("mailto:a@example.com");
        assertThat(UrlPolicy.sanitizeLink("/articles/1")).isEqualTo("/articles/1");
        assertThat(UrlPolicy.sanitizeLink("../about")).isEqualTo("../about");
        assertThat(UrlPolicy.sanitizeLink("#section")).isEqualTo("#section");
    }

    @Test
    @DisplayName("图片不允许 mailto，协议相对地址也拒绝")
    void imageRules() {
        assertThat(UrlPolicy.sanitizeImage("https://example.com/a.png")).isEqualTo("https://example.com/a.png");
        assertThat(UrlPolicy.sanitizeImage("/media/1")).isEqualTo("/media/1");
        assertThat(UrlPolicy.sanitizeImage("mailto:a@example.com")).isEmpty();
        assertThat(UrlPolicy.sanitizeLink("//evil.example.com")).isEmpty();
    }
}
