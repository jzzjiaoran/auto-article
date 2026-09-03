package com.autoarticle.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 微博热搜抓取器（真实 HTTP，失败自动降级为空列表）。
 * 数据源：微博热搜榜 JSON 接口。
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
    protected List<CrawledTopic> parse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.path("data");
        JsonNode realtime = data.path("realtime");
        List<CrawledTopic> result = new ArrayList<>();
        if (!realtime.isArray()) {
            return result;
        }
        int rank = 0;
        for (JsonNode item : realtime) {
            JsonNode word = item.path("word");
            if (word.isMissingNode() || word.asText().isBlank()) {
                continue;
            }
            if (item.path("is_ad").asBoolean(false)) {
                continue;
            }
            rank++;
            result.add(CrawledTopic.builder()
                    .title(word.asText().replace("#", "").trim())
                    .source(source())
                    .rank(rank)
                    .hotLevel(levelForRank(rank))
                    .sourceUrl(buildSearchUrl(word.asText()))
                    .build());
        }
        return result;
    }

    private String buildSearchUrl(String word) {
        return "https://s.weibo.com/weibo?q=" + java.net.URLEncoder.encode(word, java.nio.charset.StandardCharsets.UTF_8);
    }
}
