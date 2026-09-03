package com.autoarticle.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    void should_sanitize_dangerous_html() {
        String dirty = "<script>alert('xss')</script><p>Safe content</p>";
        String clean = sanitizer.sanitize(dirty);
        assertFalse(clean.contains("<script>"));
        assertTrue(clean.contains("Safe content"));
    }

    @Test
    void should_remove_event_handlers() {
        String dirty = "<p onclick=\"alert('xss')\">Click me</p>";
        String clean = sanitizer.sanitize(dirty);
        assertFalse(clean.contains("onclick"));
        assertTrue(clean.contains("Click me"));
    }

    @Test
    void should_allow_safe_tags() {
        String html = "<h1>Title</h1><p><strong>Bold</strong> and <em>italic</em></p><a href=\"https://example.com\">Link</a>";
        String clean = sanitizer.sanitize(html);
        assertTrue(clean.contains("<h1>"));
        assertTrue(clean.contains("<strong>"));
        assertTrue(clean.contains("<a href=\"https://example.com\">"));
    }

    @Test
    void should_remove_javascript_urls() {
        String dirty = "<a href=\"javascript:alert('xss')\">Click</a>";
        String clean = sanitizer.sanitize(dirty);
        assertFalse(clean.contains("javascript:"));
    }

    @Test
    void should_handle_null_input() {
        assertEquals("", sanitizer.sanitize(null));
        assertEquals("", sanitizer.markdownToHtml(null));
    }

    @Test
    void should_convert_markdown_to_html() {
        String md = "## Title\n\n**Bold text**\n\n- item1\n- item2";
        String html = sanitizer.markdownToHtml(md);
        assertTrue(html.contains("<h2>Title</h2>"));
        assertTrue(html.contains("<strong>Bold text</strong>"));
        assertTrue(html.contains("<li>item1</li>"));
    }
}
