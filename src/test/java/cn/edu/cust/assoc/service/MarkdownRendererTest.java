package cn.edu.cust.assoc.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @Test
    @DisplayName("javascript: 与 data: 链接不会出现在 href 里")
    void dangerousLinks() {
        String html = renderer.render(
                "[点我](javascript:alert(1))\n\n[数据](data:text/html,<script>alert(1)</script>)");
        assertThat(html).doesNotContain("href=\"javascript:");
        assertThat(html).doesNotContain("href=\"data:");
        assertThat(html).contains("点我");
    }

    @Test
    @DisplayName("图片地址同样过协议白名单")
    void dangerousImages() {
        String html = renderer.render("![x](data:text/html,<script>alert(1)</script>)");
        assertThat(html).doesNotContain("src=\"data:");
    }

    @Test
    @DisplayName("原文里的裸 HTML 被转义")
    void escapedHtml() {
        String html = renderer.render("<script>alert(1)</script>");
        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("正常链接与站内链接保留")
    void safeLinks() {
        String html = renderer.render("[官网](https://example.com) [站内](/articles/1)");
        assertThat(html).contains("href=\"https://example.com\"");
        assertThat(html).contains("href=\"/articles/1\"");
    }
}
