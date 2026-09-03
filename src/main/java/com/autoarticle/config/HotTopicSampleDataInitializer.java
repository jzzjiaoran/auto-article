package com.autoarticle.config;

import com.autoarticle.service.HotTopicCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 全新环境启动时自动写入示例/种子热点，保证 hot_topics 不为空、
 * 热点列表与「热点 → 生成文章」主链路可直接使用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotTopicSampleDataInitializer implements CommandLineRunner {

    private final HotTopicCollectionService hotTopicCollectionService;

    @Value("${app.hot-topic.seed-on-start:true}")
    private boolean seedOnStart;

    @Override
    public void run(String... args) {
        if (!seedOnStart) {
            return;
        }
        try {
            int seeded = hotTopicCollectionService.seedSampleIfEmpty();
            if (seeded > 0) {
                log.info("[hot-topic] seeded {} sample hot topics on startup", seeded);
            }
        } catch (Exception e) {
            log.error("[hot-topic] failed to seed sample hot topics on startup", e);
        }
    }
}
