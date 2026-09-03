package com.autoarticle.crawler;

import com.autoarticle.entity.HotTopic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 微博热搜抓取器（真实 HTTP，失败自动降级为空列表）。
 */
@Component
@RequiredArgsConstructor
public class WeiboHotTopicCrawler extends HttpJsonTopicCrawler {

    private final ObjectMapper objectMapper;

    @Override
    public String source() {
        return "weibo";
    }

    @Override
    protected String requestUrl() {
        return "https://weibo.com/ajax/side/hotSearch";
    }

    @Override
    protected List<HotTopic> parse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode realtime = root.path("data").path("realtime");
        List<HotTopic> result = new ArrayList<>();
        if (!realtime.isArray()) {
            return result;
        }
        int rank = 0;
        for (JsonNode item : realtime) {
            String word = item.path("word").asText("");
            if (word.isBlank() || item.path("is_ad").asBoolean(false)) {
                continue;
            }
            rank++;
            String title = clean(word).replace("#", "");
            if (title.isBlank()) {
                continue;
            }
            result.add(HotTopic.builder()
                    .title(title)
                    .source(source())
                    .rank(rank)
                    .hotLevel(levelForRank(rank))
                    .sourceUrl(searchUrl(title))
                    .build());
        }
        return result;
    }

    private String searchUrl(String title) {
        return "https://s.weibo.com/weibo?q=" + URLEncoder.encode(title, StandardCharsets.UTF_8);
    }
}
