package com.autoarticle.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeiboHotTopicCrawlerTest {

    private final WeiboHotTopicCrawler crawler = new WeiboHotTopicCrawler(new ObjectMapper());

    @Test
    void should_parse_realtime_topics() throws Exception {
        String body = "{\"ok\":1,\"data\":{\"realtime\":[" +
                "{\"word\":\"测试话题一\",\"num\":1000000}," +
                "{\"word\":\"#测试话题二#\",\"num\":500000}]}}";

        List<CrawledTopic> topics = crawler.parse(body);

        assertEquals(2, topics.size());
        assertEquals("测试话题一", topics.get(0).getTitle());
        assertEquals("weibo", topics.get(0).getSource());
        assertEquals(1, topics.get(0).getRank());
        assertEquals("测试话题二", topics.get(1).getTitle());
        assertEquals("high", topics.get(0).getHotLevel());
    }

    @Test
    void should_skip_ad_and_missing() throws Exception {
        String body = "{\"ok\":1,\"data\":{\"realtime\":[" +
                "{\"word\":\"\",\"num\":0}," +
                "{\"word\":\"广告\",\"is_ad\":true}]}}";

        List<CrawledTopic> topics = crawler.parse(body);

        assertTrue(topics.isEmpty());
    }
}
