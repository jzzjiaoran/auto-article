package com.autoarticle.service;

import com.autoarticle.crawler.SampleTopicsProvider;
import com.autoarticle.crawler.TopicCrawler;
import com.autoarticle.dto.HotTopicDto;
import com.autoarticle.entity.HotTopic;
import com.autoarticle.exception.ResourceNotFoundException;
import com.autoarticle.repository.HotTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotTopicService {

    private final HotTopicRepository hotTopicRepository;
    private final List<TopicCrawler> topicCrawlers;
    private final SampleTopicsProvider sampleTopicsProvider;

    @Value("${app.hot-topic.fallback-sample-enabled:true}")
    private boolean fallbackSampleEnabled;

    public Page<HotTopicDto> getTopics(String keyword, String source, String status, int page, int size) {
        return hotTopicRepository.findByFilters(keyword, source, status, PageRequest.of(page, size))
                .map(this::toDto);
    }

    public HotTopicDto getTopicById(Long id) {
        HotTopic topic = hotTopicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("热点", id));
        return toDetailDto(topic);
    }

    @Transactional
    public void refreshTopic(Long id) {
        HotTopic topic = hotTopicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("热点", id));
        log.info("Refreshing hot topic: {}", topic.getTitle());
        topic.setStatus("unused");
        hotTopicRepository.save(topic);
    }

    /**
     * 热点采集编排：@Scheduled / 手动按钮 → {@code collectHotTopics()} → 逐 TopicCrawler 抓取 → 按 title 去重 → 入库。
     * 单个平台失败不影响其它平台（逐源 try-catch + log）。返回本次新增条数。
     */
    @Transactional
    public int collectHotTopics() {
        int added = 0;
        boolean anyCrawled = false;
        for (TopicCrawler crawler : topicCrawlers) {
            try {
                List<HotTopic> fetched = crawler.fetch();
                if (fetched == null || fetched.isEmpty()) {
                    log.info("[hot-topic] crawler {} returned empty", crawler.source());
                    continue;
                }
                anyCrawled = true;
                int addedBySource = persistNewTopics(fetched);
                added += addedBySource;
                log.info("[hot-topic] crawler {} added {}", crawler.source(), addedBySource);
            } catch (Exception e) {
                log.warn("[hot-topic] crawler {} failed, continue with other sources: {}", crawler.source(), e.getMessage());
            }
        }
        if (added == 0 && !anyCrawled && fallbackSampleEnabled && hotTopicRepository.count() == 0) {
            added = seedSampleTopics();
            log.info("[hot-topic] no real crawl result, seeded {} sample topics", added);
        }
        return added;
    }

    private int persistNewTopics(List<HotTopic> fetched) {
        int added = 0;
        for (HotTopic topic : fetched) {
            if (topic.getTitle() == null || topic.getTitle().isBlank()) {
                continue;
            }
            String title = topic.getTitle().trim();
            if (hotTopicRepository.existsByTitleIgnoreCase(title)) {
                continue;
            }
            if (topic.getSource() == null || topic.getSource().isBlank()) {
                topic.setSource("other");
            }
            if (topic.getStatus() == null || topic.getStatus().isBlank()) {
                topic.setStatus("unused");
            }
            topic.setTitle(title);
            hotTopicRepository.save(topic);
            added++;
        }
        return added;
    }

    /**
     * 全新/空库时写入内置示例热点（由 {@code HotTopicSampleDataInitializer} 在启动时调用）。
     */
    @Transactional
    public int seedSampleIfEmpty() {
        if (hotTopicRepository.count() > 0) {
            return 0;
        }
        return seedSampleTopics();
    }

    private int seedSampleTopics() {
        return persistNewTopics(sampleTopicsProvider.topics());
    }

    private HotTopicDto toDto(HotTopic topic) {
        long articleCount = topic.getArticles() != null ? topic.getArticles().size() : 0;
        return HotTopicDto.builder()
                .id(topic.getId())
                .title(topic.getTitle())
                .source(topic.getSource())
                .rank(topic.getRank())
                .hotLevel(topic.getHotLevel())
                .status(topic.getStatus())
                .sourceUrl(topic.getSourceUrl())
                .collectedAt(topic.getCollectedAt())
                .articleCount(articleCount)
                .build();
    }

    private HotTopicDto toDetailDto(HotTopic topic) {
        HotTopicDto dto = toDto(topic);
        dto.setSourceUrl(topic.getSourceUrl());
        return dto;
    }
}
