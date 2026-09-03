package com.autoarticle.service;

import com.autoarticle.dto.ArticleDto;
import com.autoarticle.entity.Article;
import com.autoarticle.exception.ResourceNotFoundException;
import com.autoarticle.repository.ArticleRepository;
import com.autoarticle.util.HtmlSanitizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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

    @Test
    void should_sort_by_createdAt_desc_when_sort_is_createdAt_d() {
        mockArticlesPage();
        articleService.getArticles(null, null, "createdAt,d", 0, 10);
        assertSort(capturePageRequest(), "createdAt", Sort.Direction.DESC);
    }

    @Test
    void should_sort_by_updatedAt_desc_when_sort_is_updatedAt_d() {
        mockArticlesPage();
        articleService.getArticles(null, null, "updatedAt,d", 0, 10);
        assertSort(capturePageRequest(), "updatedAt", Sort.Direction.DESC);
    }

    @Test
    void should_sort_by_wordCount_desc_when_sort_is_wordCount_d() {
        mockArticlesPage();
        articleService.getArticles(null, null, "wordCount,d", 0, 10);
        assertSort(capturePageRequest(), "wordCount", Sort.Direction.DESC);
    }

    @Test
    void should_sort_by_createdAt_asc_when_sort_is_createdAt_a() {
        mockArticlesPage();
        articleService.getArticles(null, null, "createdAt,a", 0, 10);
        assertSort(capturePageRequest(), "createdAt", Sort.Direction.ASC);
    }

    @Test
    void should_sort_by_bare_field_using_desc_default() {
        mockArticlesPage();
        articleService.getArticles(null, null, "wordCount", 0, 10);
        assertSort(capturePageRequest(), "wordCount", Sort.Direction.DESC);
    }

    @Test
    void should_default_to_updatedAt_desc_when_sort_is_null() {
        mockArticlesPage();
        articleService.getArticles(null, null, null, 0, 10);
        assertSort(capturePageRequest(), "updatedAt", Sort.Direction.DESC);
    }

    @Test
    void should_default_to_updatedAt_desc_when_sort_is_blank() {
        mockArticlesPage();
        articleService.getArticles(null, null, "   ", 0, 10);
        assertSort(capturePageRequest(), "updatedAt", Sort.Direction.DESC);
    }

    @Test
    void should_fallback_to_updatedAt_desc_when_sort_field_unknown() {
        mockArticlesPage();
        articleService.getArticles(null, null, "title,d", 0, 10);
        assertSort(capturePageRequest(), "updatedAt", Sort.Direction.DESC);
    }

    private void mockArticlesPage() {
        when(articleRepository.findByFilters(any(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
    }

    private PageRequest capturePageRequest() {
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(articleRepository).findByFilters(any(), any(), captor.capture());
        return captor.getValue();
    }

    private void assertSort(PageRequest pageRequest, String property, Sort.Direction direction) {
        Sort sort = pageRequest.getSort();
        assertNotNull(sort, "PageRequest 必须携带 Sort");
        assertTrue(sort.isSorted(), "PageRequest 必须携带排序");
        Sort.Order order = sort.getOrderFor(property);
        assertNotNull(order, "Sort 必须包含字段: " + property);
        assertEquals(direction, order.getDirection());
    }
}
