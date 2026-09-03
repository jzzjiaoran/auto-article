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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotTopicService {

    private final HotTopicRepository hotTopicRepository;
    private final List<TopicCrawler> topicCrawlers;
    private final SampleTopicsProvider sampleTopicsProvider;

    @Value("${app.hot-topic.fallback-sample-enabled:true}")
    private boolean fallbackSampleEnabled;

    private final AtomicBoolean collecting = new AtomicBoolean(false);

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
     * 单平台失败不影响其它平台（逐源 try-catch + log）。返回本次新增条数；并发进行中返回 -1。
     */
    public int collectHotTopics() {
        if (!collecting.compareAndSet(false, true)) {
            log.warn("[hot-topic] collect skipped: another collection/seed task is running");
            return -1;
        }
        try {
            return doCollectHotTopics();
        } finally {
            collecting.set(false);
        }
    }

    private int doCollectHotTopics() {
        int added = 0;
        int skipped = 0;
        boolean anyCrawled = false;
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        for (TopicCrawler crawler : topicCrawlers) {
            try {
                List<HotTopic> fetched = crawler.fetch();
                if (fetched == null || fetched.isEmpty()) {
                    log.info("[hot-topic] crawler {} returned empty", crawler.source());
                    continue;
                }
                anyCrawled = true;
                int[] counts = persistNewTopics(fetched, startOfDay);
                added += counts[0];
                skipped += counts[1];
                log.info("[hot-topic] crawler {} added {} skipped {}", crawler.source(), counts[0], counts[1]);
            } catch (Exception e) {
                log.warn("[hot-topic] crawler {} failed, continue with other sources: {}", crawler.source(), e.getMessage());
            }
        }
        if (added == 0 && !anyCrawled && fallbackSampleEnabled && hotTopicRepository.count() == 0) {
            int[] counts = persistNewTopics(sampleTopicsProvider.topics(), startOfDay);
            added += counts[0];
            skipped += counts[1];
            log.info("[hot-topic] no real crawl result, seeded {} sample topics (skipped {})", counts[0], counts[1]);
        }
        return added;
    }

    /**
     * 全新/空库时写入内置示例热点（由 {@code HotTopicSampleDataInitializer} 在启动时调用）。
     * 与 collectHotTopics 互斥（同一 AtomicBoolean 守卫）；并发进行中返回 0。
     */
    public int seedSampleIfEmpty() {
        if (!collecting.compareAndSet(false, true)) {
            log.warn("[hot-topic] seed skipped: another collection/seed task is running");
            return 0;
        }
        try {
            if (hotTopicRepository.count() > 0) {
                return 0;
            }
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            int[] counts = persistNewTopics(sampleTopicsProvider.topics(), startOfDay);
            log.info("[hot-topic] seeded {} sample topics (skipped {})", counts[0], counts[1]);
            return counts[0];
        } finally {
            collecting.set(false);
        }
    }

    /**
     * 按 title（忽略大小写）+ 当天范围去重后入库。返回 int[0]=added、int[1]=skipped。
     * 去重查询命中即跳过；数据库唯一约束兜底冲突（uk_hot_topics_title_collected_at）捕获为 skipped，
     * 避免并发写竞态产生同日重复行，也不中断整批。
     */
    private int[] persistNewTopics(List<HotTopic> fetched, LocalDateTime startOfDay) {
        int added = 0;
        int skipped = 0;
        Set<String> seen = new HashSet<>();
        for (HotTopic topic : fetched) {
            if (topic.getTitle() == null || topic.getTitle().isBlank()) {
                continue;
            }
            String title = topic.getTitle().trim();
            String key = title.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                skipped++;
                continue;
            }
            if (hotTopicRepository.existsByTitleIgnoreCaseAndCollectedAtGreaterThanEqual(title, startOfDay)) {
                skipped++;
                continue;
            }
            if (topic.getSource() == null || topic.getSource().isBlank()) {
                topic.setSource("other");
            }
            if (topic.getStatus() == null || topic.getStatus().isBlank()) {
                topic.setStatus("unused");
            }
            topic.setTitle(title);
            try {
                hotTopicRepository.save(topic);
                added++;
            } catch (DataIntegrityViolationException e) {
                log.warn("[hot-topic] duplicate title '{}' rejected by unique constraint, counted as skipped", title);
                skipped++;
            }
        }
        return new int[]{added, skipped};
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
