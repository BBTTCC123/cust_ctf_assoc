package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.UrlPolicy;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.UrlSanitizer;
import org.springframework.stereotype.Component;

@Component
public class MarkdownRenderer {

    private final Parser parser = Parser.builder().build();
    // escapeHtml(true)：原文里的裸 HTML 会被转义，而不是原样输出。这是防 XSS 的第一道闸。
    // 协议白名单是第二道闸：javascript: / data: 等链接与图片地址会被整个丢掉。
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .escapeHtml(true)
            .sanitizeUrls(true)
            .urlSanitizer(new SafeUrlSanitizer())
            .build();

    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    static final class SafeUrlSanitizer implements UrlSanitizer {
        @Override
        public String sanitizeLinkUrl(String url) {
            return UrlPolicy.sanitizeLink(url);
        }

        @Override
        public String sanitizeImageUrl(String url) {
            return UrlPolicy.sanitizeImage(url);
        }
    }
}
