package com.autoarticle.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("h1", "h2", "h3", "h4", "h5", "h6", "pre", "code", "hr", "br", "div", "span")
            .addAttributes(":all", "class", "id")
            .addAttributes("a", "href", "title", "target", "rel")
            .addAttributes("img", "src", "alt", "title", "width", "height")
            .addAttributes("code", "class")
            .addAttributes("pre", "class");

    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.clean(html, SAFELIST);
    }

    public String markdownToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String html = basicMarkdownToHtml(markdown);
        return sanitize(html);
    }

    private String basicMarkdownToHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        String[] lines = markdown.split("\n");
        boolean inCodeBlock = false;
        StringBuilder codeBuffer = new StringBuilder();

        for (String line : lines) {
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    html.append("<pre><code>").append(codeBuffer).append("</code></pre>\n");
                    codeBuffer.setLength(0);
                    inCodeBlock = false;
                } else {
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                codeBuffer.append(line).append("\n");
                continue;
            }

            if (line.startsWith("### ")) {
                html.append("<h3>").append(processInline(line.substring(4))).append("</h3>\n");
            } else if (line.startsWith("## ")) {
                html.append("<h2>").append(processInline(line.substring(3))).append("</h2>\n");
            } else if (line.startsWith("# ")) {
                html.append("<h1>").append(processInline(line.substring(2))).append("</h1>\n");
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                html.append("<li>").append(processInline(line.substring(2))).append("</li>\n");
            } else if (line.startsWith("> ")) {
                html.append("<blockquote>").append(processInline(line.substring(2))).append("</blockquote>\n");
            } else if (line.trim().isEmpty()) {
                html.append("\n");
            } else {
                html.append("<p>").append(processInline(line)).append("</p>\n");
            }
        }
        return html.toString();
    }

    private String processInline(String text) {
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        text = text.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        text = text.replaceAll("`(.+?)`", "<code>$1</code>");
        text = text.replaceAll("\\[(.+?)\\]\\((.+?)\\)", "<a href=\"$2\" target=\"_blank\">$1</a>");
        return text;
    }
}
