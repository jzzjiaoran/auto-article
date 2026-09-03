package com.autoarticle.service;

import com.autoarticle.dto.ArticleDto;
import com.autoarticle.entity.Article;
import com.autoarticle.exception.ResourceNotFoundException;
import com.autoarticle.repository.ArticleRepository;
import com.autoarticle.util.HtmlSanitizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private com.autoarticle.repository.HotTopicRepository hotTopicRepository;
    @Mock
    private HtmlSanitizer htmlSanitizer;

    @InjectMocks
    private ArticleService articleService;

    @Test
    void should_throw_when_article_not_found() {
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> articleService.getArticleById(999L));
    }

    @Test
    void should_return_article_dto_when_found() {
        Article article = Article.builder()
                .id(1L)
                .title("Test Article")
                .status("draft")
                .wordCount(100)
                .build();
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));

        ArticleDto dto = articleService.getArticleById(1L);

        assertEquals("Test Article", dto.getTitle());
        assertEquals("draft", dto.getStatus());
    }

    @Test
    void should_update_article_content() {
        Article article = Article.builder()
                .id(1L)
                .title("Old Title")
                .content("old content")
                .status("draft")
                .build();
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(htmlSanitizer.markdownToHtml("new content")).thenReturn("<p>new content</p>");
        when(articleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArticleDto result = articleService.updateArticle(1L, "New Title", "new content", "summary", null);

        assertEquals("New Title", result.getTitle());
    }
}
