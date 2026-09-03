package com.autoarticle.service;

import com.autoarticle.dto.GenerationRequest;
import com.autoarticle.dto.TaskStatus;
import com.autoarticle.entity.Article;
import com.autoarticle.entity.GenerationTask;
import com.autoarticle.entity.HotTopic;
import com.autoarticle.exception.ResourceNotFoundException;
import com.autoarticle.repository.ArticleRepository;
import com.autoarticle.repository.GenerationTaskRepository;
import com.autoarticle.repository.HotTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final GenerationTaskRepository generationTaskRepository;
    private final ArticleRepository articleRepository;
    private final HotTopicRepository hotTopicRepository;
    private final AsyncGenerationService asyncGenerationService;

    @Transactional
    public String startGeneration(GenerationRequest request) {
        Article article;
        if (request.getTopicId() != null) {
            HotTopic topic = hotTopicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new ResourceNotFoundException("热点", request.getTopicId()));
            article = Article.builder()
                    .title(request.getTitle())
                    .content("")
                    .contentHtml("")
                    .status("draft")
                    .style(request.getStyle())
                    .length(request.getLength())
                    .prompt(request.getPrompt())
                    .hotTopic(topic)
                    .build();
        } else {
            article = Article.builder()
                    .title(request.getTitle())
                    .content("")
                    .contentHtml("")
                    .status("draft")
                    .style(request.getStyle())
                    .length(request.getLength())
                    .prompt(request.getPrompt())
                    .build();
        }
        article = articleRepository.save(article);

        String taskId = UUID.randomUUID().toString();
        GenerationTask task = GenerationTask.builder()
                .taskId(taskId)
                .status("pending")
                .message("准备生成...")
                .articleId(article.getId())
                .build();
        generationTaskRepository.save(task);

        asyncGenerationService.doGenerate(taskId, article.getId(), request);
        return taskId;
    }

    @Transactional
    public String regenerateArticle(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("文章", articleId));

        String taskId = UUID.randomUUID().toString();
        GenerationTask task = GenerationTask.builder()
                .taskId(taskId)
                .status("pending")
                .message("重新生成中...")
                .articleId(articleId)
                .build();
        generationTaskRepository.save(task);

        GenerationRequest request = GenerationRequest.builder()
                .title(article.getTitle())
                .style(article.getStyle())
                .length(article.getLength())
                .prompt(article.getPrompt())
                .topicId(article.getHotTopic() != null ? article.getHotTopic().getId() : null)
                .build();
        asyncGenerationService.doGenerate(taskId, articleId, request);
        return taskId;
    }

    public TaskStatus getTaskStatus(String taskId) {
        GenerationTask task = generationTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("任务", null));
        return TaskStatus.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .message(task.getMessage())
                .articleId(task.getArticleId())
                .build();
    }
}
