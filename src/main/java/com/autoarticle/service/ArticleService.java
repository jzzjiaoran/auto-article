package com.autoarticle.service;

import com.autoarticle.dto.ArticleDto;
import com.autoarticle.entity.Article;
import com.autoarticle.entity.HotTopic;
import com.autoarticle.exception.ResourceNotFoundException;
import com.autoarticle.repository.ArticleRepository;
import com.autoarticle.repository.HotTopicRepository;
import com.autoarticle.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final HotTopicRepository hotTopicRepository;
    private final HtmlSanitizer htmlSanitizer;

    public Page<ArticleDto> getArticles(String keyword, String status, String sort, int page, int size) {
        if (sort == null || sort.isBlank()) {
            sort = "updatedAt";
        }
        return articleRepository.findByFilters(keyword, status, sort, PageRequest.of(page, size))
                .map(this::toDto);
    }

    public ArticleDto getArticleById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("文章", id));
        return toDetailDto(article);
    }

    @Transactional
    public ArticleDto updateArticle(Long id, String title, String content, String summary, String status) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("文章", id));
        article.setTitle(title);
        article.setContent(content);
        article.setContentHtml(htmlSanitizer.markdownToHtml(content));
        article.setSummary(summary);
        if (status != null) {
            article.setStatus(status);
        }
        article.setWordCount(content != null ? content.length() : 0);
        article = articleRepository.save(article);
        log.info("Updated article: {}", article.getTitle());
        return toDto(article);
    }

    public ArticleDto toDto(Article article) {
        return ArticleDto.builder()
                .id(article.getId())
                .title(article.getTitle())
                .status(article.getStatus())
                .wordCount(article.getWordCount())
                .aiProvider(article.getAiProvider())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    private ArticleDto toDetailDto(Article article) {
        return ArticleDto.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(article.getContent())
                .contentHtml(article.getContentHtml())
                .summary(article.getSummary())
                .status(article.getStatus())
                .wordCount(article.getWordCount())
                .aiProvider(article.getAiProvider())
                .aiModel(article.getAiModel())
                .style(article.getStyle())
                .length(article.getLength())
                .hotTopicId(article.getHotTopic() != null ? article.getHotTopic().getId() : null)
                .hotTopicTitle(article.getHotTopic() != null ? article.getHotTopic().getTitle() : null)
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }
}
