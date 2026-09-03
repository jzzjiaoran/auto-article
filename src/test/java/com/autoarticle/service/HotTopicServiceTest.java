package com.autoarticle.service;

import com.autoarticle.crawler.SampleTopicsProvider;
import com.autoarticle.crawler.TopicCrawler;
import com.autoarticle.dto.HotTopicDto;
import com.autoarticle.entity.Article;
import com.autoarticle.entity.HotTopic;
import com.autoarticle.exception.ResourceNotFoundException;
import com.autoarticle.repository.HotTopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotTopicServiceTest {

    @Mock
    private HotTopicRepository hotTopicRepository;

    @Mock
    private TopicCrawler weiboCrawler;

    @Mock
    private TopicCrawler zhihuCrawler;

    private final SampleTopicsProvider sampleTopicsProvider = new SampleTopicsProvider();

    private HotTopicService hotTopicService;

    @BeforeEach
    void setUp() {
        hotTopicService = new HotTopicService(
                hotTopicRepository, List.of(weiboCrawler, zhihuCrawler), sampleTopicsProvider);
        ReflectionTestUtils.setField(hotTopicService, "fallbackSampleEnabled", true);
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

    @Test
    void collect_should_persist_crawled_topics_and_return_count() {
        when(weiboCrawler.source()).thenReturn("weibo");
        when(weiboCrawler.fetch()).thenReturn(List.of(
                HotTopic.builder().title("话题A").source("weibo").rank(1).hotLevel("high").build(),
                HotTopic.builder().title("话题B").source("weibo").rank(2).build()));
        when(zhihuCrawler.source()).thenReturn("zhihu");
        when(zhihuCrawler.fetch()).thenReturn(List.of());
        when(hotTopicRepository.existsByTitleIgnoreCaseAndCollectedAtGreaterThanEqual(anyString(), any()))
                .thenReturn(false);
        when(hotTopicRepository.save(any(HotTopic.class))).thenAnswer(inv -> inv.getArgument(0));

        int added = hotTopicService.collectHotTopics();

        assertEquals(2, added);
        ArgumentCaptor<HotTopic> captor = ArgumentCaptor.forClass(HotTopic.class);
        verify(hotTopicRepository, times(2)).save(captor.capture());
        assertEquals("话题A", captor.getAllValues().get(0).getTitle());
        assertEquals("weibo", captor.getAllValues().get(0).getSource());
        assertEquals("unused", captor.getAllValues().get(0).getStatus());
    }

    @Test
    void collect_should_skip_duplicate_titles() {
        when(weiboCrawler.source()).thenReturn("weibo");
        when(weiboCrawler.fetch()).thenReturn(List.of(
                HotTopic.builder().title("话题A").source("weibo").rank(1).build()));
        when(zhihuCrawler.source()).thenReturn("zhihu");
        when(zhihuCrawler.fetch()).thenReturn(List.of(
                HotTopic.builder().title("话题A").source("zhihu").rank(1).build()));
        when(hotTopicRepository.existsByTitleIgnoreCaseAndCollectedAtGreaterThanEqual(eq("话题A"), any()))
                .thenReturn(true);

        int added = hotTopicService.collectHotTopics();

        assertEquals(0, added);
        verify(hotTopicRepository, never()).save(any(HotTopic.class));
    }

    @Test
    void collect_should_isolate_source_failures() {
        when(weiboCrawler.source()).thenReturn("weibo");
        when(weiboCrawler.fetch()).thenThrow(new RuntimeException("network down"));
        when(zhihuCrawler.source()).thenReturn("zhihu");
        when(zhihuCrawler.fetch()).thenReturn(List.of(
                HotTopic.builder().title("知乎话题").source("zhihu").rank(1).build()));
        when(hotTopicRepository.existsByTitleIgnoreCaseAndCollectedAtGreaterThanEqual(anyString(), any()))
                .thenReturn(false);
        when(hotTopicRepository.save(any(HotTopic.class))).thenAnswer(inv -> inv.getArgument(0));

        int added = hotTopicService.collectHotTopics();

        assertEquals(1, added);
        verify(hotTopicRepository, times(1)).save(any(HotTopic.class));
    }

    @Test
    void collect_should_count_db_unique_violation_as_skipped() {
        when(weiboCrawler.source()).thenReturn("weibo");
        when(weiboCrawler.fetch()).thenReturn(List.of(
                HotTopic.builder().title("话题A").source("weibo").rank(1).build(),
                HotTopic.builder().title("话题B").source("weibo").rank(2).build()));
        when(zhihuCrawler.source()).thenReturn("zhihu");
        when(zhihuCrawler.fetch()).thenReturn(List.of());
        when(hotTopicRepository.existsByTitleIgnoreCaseAndCollectedAtGreaterThanEqual(anyString(), any()))
                .thenReturn(false);
        when(hotTopicRepository.save(any(HotTopic.class)))
                .thenAnswer(inv -> {
                    HotTopic topic = inv.getArgument(0);
                    if ("话题B".equals(topic.getTitle())) {
                        throw new DataIntegrityViolationException("duplicate key");
                    }
                    return topic;
                });

        int added = hotTopicService.collectHotTopics();

        assertEquals(1, added);
        verify(hotTopicRepository, times(2)).save(any(HotTopic.class));
    }

    @Test
    void collect_should_fallback_to_sample_when_empty_and_db_empty() {
        when(weiboCrawler.source()).thenReturn("weibo");
        when(weiboCrawler.fetch()).thenReturn(List.of());
        when(zhihuCrawler.source()).thenReturn("zhihu");
        when(zhihuCrawler.fetch()).thenReturn(List.of());
        when(hotTopicRepository.count()).thenReturn(0L);
        when(hotTopicRepository.existsByTitleIgnoreCaseAndCollectedAtGreaterThanEqual(anyString(), any()))
                .thenReturn(false);
        when(hotTopicRepository.save(any(HotTopic.class))).thenAnswer(inv -> inv.getArgument(0));

        int added = hotTopicService.collectHotTopics();

        assertTrue(added > 0);
        verify(hotTopicRepository, atLeastOnce()).save(any(HotTopic.class));
    }

    @Test
    void seed_sample_should_only_run_when_db_empty() {
        when(hotTopicRepository.count()).thenReturn(0L);
        when(hotTopicRepository.existsByTitleIgnoreCaseAndCollectedAtGreaterThanEqual(anyString(), any()))
                .thenReturn(false);
        when(hotTopicRepository.save(any(HotTopic.class))).thenAnswer(inv -> inv.getArgument(0));

        int seeded = hotTopicService.seedSampleIfEmpty();

        assertTrue(seeded > 0);
        verify(hotTopicRepository, atLeastOnce()).save(any(HotTopic.class));
    }

    @Test
    void seed_sample_should_not_run_when_db_not_empty() {
        when(hotTopicRepository.count()).thenReturn(5L);

        int seeded = hotTopicService.seedSampleIfEmpty();

        assertEquals(0, seeded);
        verify(hotTopicRepository, never()).save(any(HotTopic.class));
    }

    @Test
    void collect_should_skip_when_already_running() {
        AtomicBoolean collecting = (AtomicBoolean) ReflectionTestUtils.getField(hotTopicService, "collecting");
        assertNotNull(collecting);
        collecting.set(true);

        int added = hotTopicService.collectHotTopics();

        assertEquals(-1, added);
        verify(weiboCrawler, never()).fetch();
        verify(zhihuCrawler, never()).fetch();
    }

    @Test
    void seed_should_skip_when_collect_running() {
        AtomicBoolean collecting = (AtomicBoolean) ReflectionTestUtils.getField(hotTopicService, "collecting");
        assertNotNull(collecting);
        collecting.set(true);

        int seeded = hotTopicService.seedSampleIfEmpty();

        assertEquals(0, seeded);
        verify(hotTopicRepository, never()).save(any(HotTopic.class));
    }

    @Test
    void guard_should_release_after_collect_finishes() {
        when(weiboCrawler.source()).thenReturn("weibo");
        when(weiboCrawler.fetch()).thenReturn(List.of());
        when(zhihuCrawler.source()).thenReturn("zhihu");
        when(zhihuCrawler.fetch()).thenReturn(List.of());
        when(hotTopicRepository.count()).thenReturn(0L);
        when(hotTopicRepository.existsByTitleIgnoreCaseAndCollectedAtGreaterThanEqual(anyString(), any()))
                .thenReturn(false);
        when(hotTopicRepository.save(any(HotTopic.class))).thenAnswer(inv -> inv.getArgument(0));

        hotTopicService.collectHotTopics();
        int second = hotTopicService.collectHotTopics();

        assertTrue(second >= 0);
    }
}
