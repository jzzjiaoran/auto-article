package com.autoarticle.service;

import com.autoarticle.crawler.CrawledTopic;
import com.autoarticle.crawler.SampleTopicsProvider;
import com.autoarticle.crawler.TopicCrawler;
import com.autoarticle.dto.HotTopicCollectResult;
import com.autoarticle.entity.HotTopic;
import com.autoarticle.repository.HotTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class HotTopicCollectionService {

    private final HotTopicRepository hotTopicRepository;
    private final List<TopicCrawler> topicCrawlers;
    private final SampleTopicsProvider sampleTopicsProvider;

    @Value("${app.hot-topic.collect.crawler-enabled:true}")
    private boolean crawlerEnabled;

    @Value("${app.hot-topic.collect.fallback-sample-enabled:true}")
    private boolean fallbackSampleEnabled;

    private final AtomicBoolean collecting = new AtomicBoolean(false);

    /**
     * 定时/手动触发：抓取并入库。@Scheduled → collectHotTopics() → TopicCrawler 抓取 → 去重 → 入库。
     * 网络抓取不放在事务内，仅持久化调用使用仓储自身的事务，避免长时间占用数据库连接。
     */
    public HotTopicCollectResult collectHotTopics() {
        if (!collecting.compareAndSet(false, true)) {
            log.warn("[hot-topic] collect skipped, another collection is running");
            return HotTopicCollectResult.busy();
        }
        try {
            return doCollect();
        } finally {
            collecting.set(false);
        }
    }

    private HotTopicCollectResult doCollect() {
        HotTopicCollectResult result = new HotTopicCollectResult();
        Set<String> seenKeys = new HashSet<>();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        boolean anyCrawled = false;

        for (TopicCrawler crawler : enabledCrawlers()) {
            List<CrawledTopic> items;
            try {
                items = crawler.fetch();
            } catch (Exception e) {
                log.warn("[hot-topic] crawler {} failed: {}", crawler.source(), e.getMessage());
                result.getSourceMessages().add(crawler.source() + " 抓取失败");
                continue;
            }
            if (items == null || items.isEmpty()) {
                result.getSourceMessages().add(crawler.source() + " 无数据");
                continue;
            }
            anyCrawled = true;
            int addedBySource = 0;
            for (CrawledTopic item : items) {
                if (item.getTitle() == null || item.getTitle().isBlank()) {
                    result.setSkipped(result.getSkipped() + 1);
                    continue;
                }
                String source = normalizeSource(item.getSource() != null ? item.getSource() : crawler.source());
                String title = item.getTitle().trim();
                String key = title.toLowerCase(Locale.ROOT);
                if (!seenKeys.add(key)) {
                    result.setSkipped(result.getSkipped() + 1);
                    continue;
                }
                if (hotTopicRepository.existsSameDay(title, startOfDay)) {
                    result.setSkipped(result.getSkipped() + 1);
                    continue;
                }
                hotTopicRepository.save(toEntity(item, source, title));
                addedBySource++;
            }
            result.setAdded(result.getAdded() + addedBySource);
            result.getSourceMessages().add(crawler.source() + " 新增 " + addedBySource);
            log.info("[hot-topic] crawler {} added {}", crawler.source(), addedBySource);
        }

        // 全新环境且没有任何真实抓取结果时回退到内置示例/种子数据，保证列表页与生成链路有数据可用。
        if (!anyCrawled && result.getAdded() == 0 && fallbackSampleEnabled
                && hotTopicRepository.count() == 0) {
            int added = saveSampleTopics(startOfDay);
            result.setAdded(result.getAdded() + added);
            result.setSampleFallbackUsed(true);
            log.info("[hot-topic] no real crawl result, seeded {} sample topics", added);
        }
        return result;
    }

    /**
     * 启动/空库时写入内置示例数据，确保全新环境 hot_topics 不为空。
     */
    @Transactional
    public int seedSampleIfEmpty() {
        if (hotTopicRepository.count() > 0) {
            return 0;
        }
        return saveSampleTopics(LocalDate.now().atStartOfDay());
    }

    private int saveSampleTopics(LocalDateTime startOfDay) {
        int added = 0;
        for (CrawledTopic item : sampleTopicsProvider.topics()) {
            String source = normalizeSource(item.getSource());
            String title = item.getTitle() == null ? "" : item.getTitle().trim();
            if (title.isBlank()) {
                continue;
            }
            if (hotTopicRepository.existsSameDay(title, startOfDay)) {
                continue;
            }
            hotTopicRepository.save(toEntity(item, source, title));
            added++;
        }
        return added;
    }

    private HotTopic toEntity(CrawledTopic item, String source, String title) {
        Integer rank = item.getRank() != null ? item.getRank() : 0;
        return HotTopic.builder()
                .title(title)
                .source(source)
                .rank(rank)
                .hotLevel(resolveHotLevel(item, rank))
                .status("unused")
                .sourceUrl(item.getSourceUrl())
                .build();
    }

    private String resolveHotLevel(CrawledTopic item, int rank) {
        if (item.getHotLevel() != null && !item.getHotLevel().isBlank()) {
            return item.getHotLevel();
        }
        if (rank > 0 && rank <= 5) {
            return "high";
        }
        if (rank <= 15) {
            return "middle";
        }
        return "normal";
    }

    private String normalizeSource(String source) {
        return source == null ? "other" : source.trim().toLowerCase(Locale.ROOT);
    }

    private List<TopicCrawler> enabledCrawlers() {
        if (!crawlerEnabled) {
            return List.of();
        }
        return topicCrawlers;
    }
}
