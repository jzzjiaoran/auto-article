package com.autoarticle.repository;

import com.autoarticle.entity.Article;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class ArticleRepositorySortTest {

    private static final String TITLE_A = "A-先创建-后更新-字数多";
    private static final String TITLE_B = "B-后创建-先更新-字数少";

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Article articleA;
    private Article articleB;

    @BeforeEach
    void setUp() {
        articleA = article(TITLE_A, 1000);
        articleB = article(TITLE_B, 100);

        // Article.createdAt is annotated with @CreationTimestamp, which ignores any explicit
        // value on INSERT and stamps ~now(). Overwrite the created_at column directly so the two
        // rows carry distinct, deterministic timestamps and the ordering assertion no longer
        // depends on insertion order or clock resolution.
        overrideCreatedAt(articleA.getId(), LocalDateTime.of(2026, 1, 1, 10, 0));
        overrideCreatedAt(articleB.getId(), LocalDateTime.of(2026, 9, 1, 9, 0));
    }

    @Test
    void should_order_by_createdAt_desc() {
        Page<Article> result = findBySort("createdAt");

        assertEquals(List.of(TITLE_B, TITLE_A), titles(result));
    }

    @Test
    void should_order_by_updatedAt_desc() throws InterruptedException {
        touch(articleB);
        Thread.sleep(5);
        touch(articleA);

        Page<Article> result = findBySort("updatedAt");

        assertEquals(List.of(TITLE_A, TITLE_B), titles(result));
    }

    @Test
    void should_order_by_wordCount_desc() {
        Page<Article> result = findBySort("wordCount");

        assertEquals(List.of(TITLE_A, TITLE_B), titles(result));
    }

    private Page<Article> findBySort(String property) {
        return articleRepository.findByFilters(null, null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, property)));
    }

    private List<String> titles(Page<Article> page) {
        return page.getContent().stream().map(Article::getTitle).toList();
    }

    private Article article(String title, int wordCount) {
        return articleRepository.saveAndFlush(Article.builder()
                .title(title)
                .status("draft")
                .wordCount(wordCount)
                .build());
    }

    private void overrideCreatedAt(Long id, LocalDateTime createdAt) {
        jdbcTemplate.update("UPDATE articles SET created_at = ? WHERE id = ?", createdAt, id);
    }

    private void touch(Article article) {
        article.setContent(article.getTitle() + "-updated");
        articleRepository.saveAndFlush(article);
    }
}
