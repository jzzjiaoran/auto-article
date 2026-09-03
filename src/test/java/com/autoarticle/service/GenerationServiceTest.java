package com.autoarticle.service;

import com.autoarticle.dto.TaskStatus;
import com.autoarticle.entity.GenerationTask;
import com.autoarticle.repository.GenerationTaskRepository;
import com.autoarticle.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerationTaskServiceTest {

    @Mock
    private GenerationTaskRepository generationTaskRepository;
    @Mock
    private com.autoarticle.repository.ArticleRepository articleRepository;
    @Mock
    private com.autoarticle.repository.HotTopicRepository hotTopicRepository;
    @Mock
    private com.autoarticle.util.HtmlSanitizer htmlSanitizer;

    @InjectMocks
    private GenerationService generationService;

    @Test
    void should_throw_when_task_not_found() {
        when(generationTaskRepository.findByTaskId("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> generationService.getTaskStatus("nonexistent"));
    }

    @Test
    void should_return_task_status() {
        GenerationTask task = GenerationTask.builder()
                .taskId("abc")
                .status("completed")
                .message("done")
                .articleId(1L)
                .build();
        when(generationTaskRepository.findByTaskId("abc")).thenReturn(Optional.of(task));

        TaskStatus status = generationService.getTaskStatus("abc");

        assertEquals("abc", status.getTaskId());
        assertEquals("completed", status.getStatus());
        assertEquals(1L, status.getArticleId());
    }

    @Test
    void should_throw_when_article_not_found_on_regenerate() {
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> generationService.regenerateArticle(999L));
    }
}
