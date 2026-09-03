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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "updatedAt", "wordCount");

    private final ArticleRepository articleRepository;
    private final HotTopicRepository hotTopicRepository;
    private final HtmlSanitizer htmlSanitizer;

    public Page<ArticleDto> getArticles(String keyword, String status, String sort, int page, int size) {
        return articleRepository.findByFilters(keyword, status, PageRequest.of(page, size, resolveSort(sort)))
                .map(this::toDto);
    }

    /**
     * 解析前端排序参数 {@code 字段,方向}（如 createdAt,d / wordCount,d），
     * 仅允许白名单字段，方向可选 asc/a 或 desc/d，缺省为倒序；非法值回退到 updatedAt 倒序。
     */
    private Sort resolveSort(String sort) {
        String field = null;
        Sort.Direction direction = Sort.Direction.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            field = parts[0].trim();
            if (parts.length > 1) {
                String raw = parts[1].trim();
                if ("asc".equalsIgnoreCase(raw) || "a".equalsIgnoreCase(raw)) {
                    direction = Sort.Direction.ASC;
                }
            }
        }
        if (field == null || !SORTABLE_FIELDS.contains(field)) {
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }
        return Sort.by(direction, field);
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
