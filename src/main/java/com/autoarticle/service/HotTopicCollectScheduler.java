package com.autoarticle.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.crawler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HotTopicCollectScheduler {

    private final HotTopicService hotTopicService;

    /**
     * 定时采集网络热点（ADR-007：Spring Scheduling）。
     * cron 通过 {@code app.crawler.cron} 配置（默认每 30 分钟），停用定时任务可将 app.crawler.enabled 置为 false。
     */
    @Scheduled(cron = "${app.crawler.cron:0 */30 * * * *}")
    public void collect() {
        try {
            int added = hotTopicService.collectHotTopics();
            log.info("[hot-topic] scheduled collect finished, added {}", added);
        } catch (Exception e) {
            log.error("[hot-topic] scheduled collect failed", e);
        }
    }
}
