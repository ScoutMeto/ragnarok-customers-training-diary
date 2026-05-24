package com.ragnarok.ragnarok_customers_training_diary.coach;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

/**
 * Wrapper kolem commonmark-java. Bezpečné renderování markdown do HTML.
 * CommonMark spec — žádné inline HTML (escape) → safe ze stranou XSS pro běžný markdown.
 */
@Component
public class MarkdownRenderer {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .escapeHtml(true)   // raw HTML v markdownu je escapováno (XSS safety)
            .build();

    public String renderToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) return "";
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }
}
