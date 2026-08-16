package com.sleekydz86.finsight.core.board.markdown;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarkdownRenderingService {

    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create(), AutolinkExtension.create());

    private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            .escapeHtml(false)
            .build();
    private final TextContentRenderer textRenderer = TextContentRenderer.builder().build();

    private final PolicyFactory htmlPolicy = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS)
            .and(Sanitizers.IMAGES)
            .and(new HtmlPolicyBuilder()
                    .allowElements("table", "thead", "tbody", "tr", "th", "td")
                    .toFactory());

    public MarkdownRenderResult render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return new MarkdownRenderResult("", "");
        }
        Node document = parser.parse(markdown);
        String rawHtml = htmlRenderer.render(document);
        String safeHtml = htmlPolicy.sanitize(rawHtml);
        String plain = textRenderer.render(document).trim();
        return new MarkdownRenderResult(safeHtml, plain);
    }
}
