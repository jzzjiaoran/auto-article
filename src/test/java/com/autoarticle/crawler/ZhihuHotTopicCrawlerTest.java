package com.autoarticle.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZhihuHotTopicCrawlerTest {

    private final ZhihuHotTopicCrawler crawler = new ZhihuHotTopicCrawler(new ObjectMapper());

    @Test
    void should_parse_hot_list_targets() throws Exception {
        String body = "{\"data\":[" +
                "{\"target\":{\"title\":\"如何评价新技术？\",\"type\":\"question\",\"id\":\"123\"}}," +
                "{\"target\":{\"title\":\"深度文章\",\"type\":\"article\",\"id\":\"456\"}}]}";

        List<CrawledTopic> topics = crawler.parse(body);

        assertEquals(2, topics.size());
        assertEquals("如何评价新技术？", topics.get(0).getTitle());
        assertEquals("zhihu", topics.get(0).getSource());
        assertEquals("https://www.zhihu.com/question/123", topics.get(0).getSourceUrl());
        assertEquals("https://zhuanlan.zhihu.com/p/456", topics.get(1).getSourceUrl());
    }
}
