package cn.edu.cust.assoc.service;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

@Component
public class MarkdownRenderer {

    private final Parser parser = Parser.builder().build();
    // escapeHtml(true)：原文里的裸 HTML 会被转义，而不是原样输出。这是防 XSS 的第一道闸。
    private final HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();

    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }
}
