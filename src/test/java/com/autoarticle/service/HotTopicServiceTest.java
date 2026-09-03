package com.autoarticle.service;

import com.autoarticle.dto.HotTopicDto;
import com.autoarticle.entity.Article;
import com.autoarticle.entity.HotTopic;
import com.autoarticle.exception.ResourceNotFoundException;
import com.autoarticle.repository.HotTopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotTopicServiceTest {

    @Mock
    private HotTopicRepository hotTopicRepository;

    @Mock
    private HotTopicCollectionService hotTopicCollectionService;

    @InjectMocks
    private HotTopicService hotTopicService;

    @Test
    void should_delegate_collect_to_collection_service() {
        when(hotTopicCollectionService.collectHotTopics()).thenReturn(new com.autoarticle.dto.HotTopicCollectResult());

        var result = hotTopicService.collectHotTopics();

        assertNotNull(result);
        verify(hotTopicCollectionService, times(1)).collectHotTopics();
    }

    @Test
    void should_throw_when_topic_not_found() {
        when(hotTopicRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> hotTopicService.getTopicById(999L));
    }

    @Test
    void should_return_topic_dto_with_articles() {
        Article article = Article.builder().id(1L).title("Article").build();
        HotTopic topic = HotTopic.builder()
                .id(1L)
                .title("Hot Topic")
                .source("weibo")
                .rank(1)
                .hotLevel("high")
                .status("unused")
                .articles(List.of(article))
                .build();
        when(hotTopicRepository.findById(1L)).thenReturn(Optional.of(topic));

        HotTopicDto dto = hotTopicService.getTopicById(1L);

        assertEquals("Hot Topic", dto.getTitle());
        assertEquals("weibo", dto.getSource());
        assertEquals(1, dto.getArticleCount());
    }

    @Test
    void should_refresh_topic() {
        HotTopic topic = HotTopic.builder().id(1L).title("Topic").status("generated").build();
        when(hotTopicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(hotTopicRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        hotTopicService.refreshTopic(1L);

        assertEquals("unused", topic.getStatus());
    }

    @Test
    void should_return_paged_topics() {
        HotTopic topic = HotTopic.builder()
                .id(1L)
                .title("Topic")
                .source("zhihu")
                .status("unused")
                .build();
        when(hotTopicRepository.findByFilters(any(), any(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(topic)));

        var page = hotTopicService.getTopics("keyword", "zhihu", "unused", 0, 10);

        assertEquals(1, page.getTotalElements());
        assertEquals("Topic", page.getContent().get(0).getTitle());
    }
}
