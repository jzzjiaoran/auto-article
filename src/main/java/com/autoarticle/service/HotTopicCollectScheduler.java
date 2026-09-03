package com.autoarticle.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.hot-topic.collect", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HotTopicCollectScheduler {

    private final HotTopicService hotTopicService;

    /**
     * 每小时定时采集网络热点（ADR-007：Spring Scheduling）。
     * cron 可通过 app.hot-topic.collect.cron 覆盖；通过 app.hot-topic.collect.enabled=false 可停用定时任务。
     */
    @Scheduled(cron = "${app.hot-topic.collect.cron:0 0 * * * *}")
    public void collect() {
        try {
            var result = hotTopicService.collectHotTopics();
            log.info("[hot-topic] scheduled collect result: {}", result.toMessage());
        } catch (Exception e) {
            log.error("[hot-topic] scheduled collect failed", e);
        }
    }
}
