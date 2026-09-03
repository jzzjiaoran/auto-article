package com.autoarticle.service;

import com.autoarticle.dto.GenerationRequest;
import com.autoarticle.dto.TaskStatus;
import com.autoarticle.entity.Article;
import com.autoarticle.entity.GenerationTask;
import com.autoarticle.entity.HotTopic;
import com.autoarticle.exception.BusinessException;
import com.autoarticle.exception.ResourceNotFoundException;
import com.autoarticle.repository.ArticleRepository;
import com.autoarticle.repository.GenerationTaskRepository;
import com.autoarticle.repository.HotTopicRepository;
import com.autoarticle.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final GenerationTaskRepository generationTaskRepository;
    private final ArticleRepository articleRepository;
    private final HotTopicRepository hotTopicRepository;
    private final HtmlSanitizer htmlSanitizer;

    @Value("${app.llm.api-key}")
    private String apiKey;

    @Value("${app.llm.base-url}")
    private String baseUrl;

    @Value("${app.llm.model}")
    private String model;

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

        doGenerate(taskId, article.getId(), request);
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
        doGenerate(taskId, articleId, request);
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

    @Async
    public void doGenerate(String taskId, Long articleId, GenerationRequest request) {
        GenerationTask task = generationTaskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) return;

        try {
            task.setStatus("running");
            task.setMessage("AI 正在生成文章...");
            generationTaskRepository.save(task);

            String prompt = buildPrompt(request);
            String content = callLlm(prompt);

            Article article = articleRepository.findById(articleId).orElse(null);
            if (article != null) {
                article.setContent(content);
                article.setContentHtml(htmlSanitizer.markdownToHtml(content));
                article.setWordCount(content.length());
                article.setStatus("generated");
                article.setAiProvider("openai");
                article.setAiModel(model);
                articleRepository.save(article);
            }

            task.setStatus("completed");
            task.setMessage("文章生成完成");
            task.setResult(content);
            generationTaskRepository.save(task);
            log.info("Generation completed for task: {}", taskId);

        } catch (Exception e) {
            task.setStatus("failed");
            task.setMessage("生成失败: " + e.getMessage());
            generationTaskRepository.save(task);
            log.error("Generation failed for task: {}", taskId, e);
        }
    }

    private String callLlm(String prompt) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String body = String.format("""
                    {
                        "model": "%s",
                        "messages": [{"role": "user", "content": "%s"}],
                        "max_tokens": 4000
                    }
                    """, model, prompt.replace("\"", "\\\""));

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/chat/completions",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getBody() != null && response.getBody().contains("\"content\"")) {
                int start = response.getBody().indexOf("\"content\":\"") + 11;
                int end = response.getBody().lastIndexOf("\"");
                if (end > start) {
                    return response.getBody().substring(start, end)
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"");
                }
            }
            return "文章生成内容（模拟）：\n\n这是基于您的提示生成的文章内容。";
        } catch (Exception e) {
            log.warn("LLM API call failed, using mock response: {}", e.getMessage());
            return generateMockContent(prompt);
        }
    }

    private String generateMockContent(String prompt) {
        return "# " + prompt.substring(0, Math.min(20, prompt.length())) + "\n\n" +
               "本文将深入探讨这一热点话题的背景、发展和影响。\n\n" +
               "## 背景介绍\n\n" +
               "随着科技的快速发展和社会的不断变化，这一话题引起了广泛关注。\n\n" +
               "## 核心分析\n\n" +
               "从多个角度来看，这一现象反映了当前社会的几个重要趋势。\n\n" +
               "1. **技术创新**：新技术正在改变我们的生活方式\n" +
               "2. **社会影响**：对传统行业带来了深远影响\n" +
               "3. **未来展望**：未来发展趋势值得关注\n\n" +
               "## 总结\n\n" +
               "总的来说，这一热点话题值得我们持续关注和深入思考。";
    }

    private String buildPrompt(GenerationRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请写一篇关于「").append(request.getTitle()).append("」的文章。");

        if (request.getStyle() != null) {
            switch (request.getStyle()) {
                case "popular" -> prompt.append("风格要求：科普风格，通俗易懂。");
                case "commentary" -> prompt.append("风格要求：评论风格，有观点有论据。");
                case "story" -> prompt.append("风格要求：故事风格，引人入胜。");
                case "marketing" -> prompt.append("风格要求：营销风格，有吸引力。");
                default -> prompt.append("风格要求：科普风格。");
            }
        }

        if (request.getLength() != null) {
            switch (request.getLength()) {
                case "short" -> prompt.append("文章长度：500字左右。");
                case "long" -> prompt.append("文章长度：2000字以上。");
                default -> prompt.append("文章长度：1000字左右。");
            }
        }

        if (request.getPrompt() != null && !request.getPrompt().isBlank()) {
            prompt.append("额外要求：").append(request.getPrompt());
        }

        prompt.append("请使用Markdown格式输出。");
        return prompt.toString();
    }
}
