package com.autoarticle.service;

import com.autoarticle.dto.DashboardStats;
import com.autoarticle.entity.Article;
import com.autoarticle.repository.ArticleRepository;
import com.autoarticle.repository.HotTopicRepository;
import com.autoarticle.repository.PlatformAccountRepository;
import com.autoarticle.repository.PublishRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private HotTopicRepository hotTopicRepository;
    @Mock
    private PlatformAccountRepository platformAccountRepository;
    @Mock
    private PublishRecordRepository publishRecordRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void should_return_stats_when_getStats() {
        when(articleRepository.count()).thenReturn(10L);
        when(hotTopicRepository.count()).thenReturn(20L);
        when(platformAccountRepository.count()).thenReturn(5L);
        when(publishRecordRepository.count()).thenReturn(30L);

        DashboardStats stats = dashboardService.getStats();

        assertEquals(10L, stats.getArticleCount());
        assertEquals(20L, stats.getHotTopicCount());
        assertEquals(5L, stats.getAccountCount());
        assertEquals(30L, stats.getPublishCount());
    }

    @Test
    void should_return_empty_when_no_articles() {
        when(articleRepository.findTop5ByOrderByUpdatedAtDesc()).thenReturn(java.util.List.of());

        var recent = dashboardService.getRecentArticles();

        assertTrue(recent.isEmpty());
    }
}
