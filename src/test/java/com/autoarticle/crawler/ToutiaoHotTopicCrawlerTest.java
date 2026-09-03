package com.autoarticle.crawler;

import com.autoarticle.entity.HotTopic;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToutiaoHotTopicCrawlerTest {

    private final ToutiaoHotTopicCrawler crawler = new ToutiaoHotTopicCrawler(new ObjectMapper());

    @Test
    void should_parse_hot_board() throws Exception {
        String body = "{\"data\":[" +
                "{\"Title\":\"头条热点一\",\"Url\":\"https://www.toutiao.com/article/1\"}," +
                "{\"Title\":\"头条热点二\",\"Url\":\"https://www.toutiao.com/article/2\"}]}";

        List<HotTopic> topics = crawler.parse(body);

        assertEquals(2, topics.size());
        assertEquals("头条热点一", topics.get(0).getTitle());
        assertEquals("toutiao", topics.get(0).getSource());
        assertEquals("https://www.toutiao.com/article/1", topics.get(0).getSourceUrl());
    }

    @Test
    void should_return_empty_on_missing_array() throws Exception {
        List<HotTopic> topics = crawler.parse("{\"data\":{}}");
        assertTrue(topics.isEmpty());
    }

    @Test
    void should_strip_html_from_title() throws Exception {
        String body = "{\"data\":[{\"Title\":\"<a>热点</a>\",\"Url\":\"https://www.toutiao.com/article/3\"}]}";
        List<HotTopic> topics = crawler.parse(body);
        assertEquals("热点", topics.get(0).getTitle());
    }
}
