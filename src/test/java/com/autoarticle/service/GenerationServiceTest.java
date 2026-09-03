package com.autoarticle.service;

import com.autoarticle.dto.GenerationRequest;
import com.autoarticle.dto.TaskStatus;
import com.autoarticle.entity.GenerationTask;
import com.autoarticle.entity.HotTopic;
import com.autoarticle.repository.GenerationTaskRepository;
import com.autoarticle.repository.ArticleRepository;
import com.autoarticle.repository.HotTopicRepository;
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
class GenerationServiceTest {

    @Mock
    private GenerationTaskRepository generationTaskRepository;
    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private HotTopicRepository hotTopicRepository;
    @Mock
    private AsyncGenerationService asyncGenerationService;

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

    @Test
    void should_throw_when_topic_not_found_on_start() {
        GenerationRequest request = GenerationRequest.builder()
                .title("Test Article")
                .topicId(999L)
                .style("popular")
                .length("medium")
                .build();
        when(hotTopicRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> generationService.startGeneration(request));
    }

    @Test
    void should_start_generation_and_call_async() {
        when(articleRepository.save(any())).thenAnswer(inv -> {
            var a = inv.getArgument(0);
            var idField = a.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(a, 1L);
            return a;
        });
        when(generationTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GenerationRequest request = GenerationRequest.builder()
                .title("Test Article")
                .style("popular")
                .length("medium")
                .build();

        String taskId = generationService.startGeneration(request);

        assertNotNull(taskId);
        verify(asyncGenerationService).doGenerate(eq(taskId), eq(1L), any(GenerationRequest.class));
    }
}
