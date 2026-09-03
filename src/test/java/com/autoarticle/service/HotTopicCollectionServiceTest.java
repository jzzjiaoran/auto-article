package com.autoarticle.service;

import com.autoarticle.crawler.CrawledTopic;
import com.autoarticle.crawler.SampleTopicsProvider;
import com.autoarticle.crawler.TopicCrawler;
import com.autoarticle.dto.HotTopicCollectResult;
import com.autoarticle.entity.HotTopic;
import com.autoarticle.repository.HotTopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotTopicCollectionServiceTest {

    @Mock
    private HotTopicRepository hotTopicRepository;

    @Mock
    private TopicCrawler weiboCrawler;

    private final SampleTopicsProvider sampleTopicsProvider = new SampleTopicsProvider();

    private HotTopicCollectionService service;

    @BeforeEach
    void setUp() {
        service = new HotTopicCollectionService(hotTopicRepository, List.of(weiboCrawler), sampleTopicsProvider);
        ReflectionTestUtils.setField(service, "crawlerEnabled", true);
        ReflectionTestUtils.setField(service, "fallbackSampleEnabled", true);
    }

    @Test
    void should_collect_and_persist_crawled_topics() {
        when(weiboCrawler.source()).thenReturn("weibo");
        when(weiboCrawler.fetch()).thenReturn(List.of(
                CrawledTopic.builder().title("话题A").source("weibo").rank(1).hotLevel("high").build(),
                CrawledTopic.builder().title("话题B").source("weibo").rank(2).build()));
        when(hotTopicRepository.existsSameDay(anyString(), any())).thenReturn(false);
        when(hotTopicRepository.save(any(HotTopic.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.collectHotTopics();

        assertEquals(2, result.getAdded());
        assertEquals(0, result.getSkipped());
        assertFalse(result.isSampleFallbackUsed());
        ArgumentCaptor<HotTopic> captor = ArgumentCaptor.forClass(HotTopic.class);
        verify(hotTopicRepository, times(2)).save(captor.capture());
        List<HotTopic> saved = captor.getAllValues();
        assertEquals("话题A", saved.get(0).getTitle());
        assertEquals("weibo", saved.get(0).getSource());
        assertEquals("high", saved.get(0).getHotLevel());
        assertEquals("unused", saved.get(0).getStatus());
        assertEquals("high", saved.get(1).getHotLevel());
    }

    @Test
    void should_skip_topics_already_collected_same_day() {
        when(weiboCrawler.source()).thenReturn("weibo");
        when(weiboCrawler.fetch()).thenReturn(List.of(
                CrawledTopic.builder().title("话题A").source("weibo").rank(1).build()));
        when(hotTopicRepository.existsSameDay(eq("话题A"), any())).thenReturn(true);

        var result = service.collectHotTopics();

        assertEquals(0, result.getAdded());
        assertEquals(1, result.getSkipped());
        verify(hotTopicRepository, never()).save(any(HotTopic.class));
    }

    @Test
    void should_fallback_to_sample_data_when_crawl_empty_and_db_empty() {
        when(weiboCrawler.source()).thenReturn("weibo");
        when(weiboCrawler.fetch()).thenReturn(List.of());
        when(hotTopicRepository.count()).thenReturn(0L);
        when(hotTopicRepository.existsSameDay(anyString(), any())).thenReturn(false);
        when(hotTopicRepository.save(any(HotTopic.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.collectHotTopics();

        assertTrue(result.isSampleFallbackUsed());
        assertTrue(result.getAdded() > 0);
        ArgumentCaptor<HotTopic> captor = ArgumentCaptor.forClass(HotTopic.class);
        verify(hotTopicRepository, atLeastOnce()).save(captor.capture());
        assertNotNull(captor.getValue().getTitle());
        assertEquals("unused", captor.getValue().getStatus());
    }

    @Test
    void should_not_insert_duplicates_in_same_run() {
        when(weiboCrawler.source()).thenReturn("weibo");
        CrawledTopic dup = CrawledTopic.builder().title("话题A").source("weibo").rank(1).build();
        when(weiboCrawler.fetch()).thenReturn(List.of(dup, dup));
        when(hotTopicRepository.existsSameDay(anyString(), any())).thenReturn(false);
        when(hotTopicRepository.save(any(HotTopic.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.collectHotTopics();

        assertEquals(1, result.getAdded());
        assertEquals(1, result.getSkipped());
        verify(hotTopicRepository, times(1)).save(any(HotTopic.class));
    }

    @Test
    void should_seed_sample_when_db_empty_only() {
        when(hotTopicRepository.count()).thenReturn(0L);
        when(hotTopicRepository.existsSameDay(anyString(), any())).thenReturn(false);
        when(hotTopicRepository.save(any(HotTopic.class))).thenAnswer(inv -> inv.getArgument(0));

        int seeded = service.seedSampleIfEmpty();

        assertTrue(seeded > 0);
        verify(hotTopicRepository, atLeastOnce()).save(any(HotTopic.class));
    }

    @Test
    void should_not_seed_sample_when_db_not_empty() {
        when(hotTopicRepository.count()).thenReturn(5L);

        int seeded = service.seedSampleIfEmpty();

        assertEquals(0, seeded);
        verify(hotTopicRepository, never()).save(any(HotTopic.class));
    }

    @Test
    void should_return_busy_when_collection_already_running() {
        when(weiboCrawler.source()).thenReturn("weibo");
        when(weiboCrawler.fetch()).thenAnswer(inv -> {
            var busy = service.collectHotTopics();
            assertTrue(busy.isInterrupted());
            return List.of();
        });

        var result = service.collectHotTopics();

        assertFalse(result.isInterrupted());
        verify(hotTopicRepository, atLeastOnce()).count();
    }
}
