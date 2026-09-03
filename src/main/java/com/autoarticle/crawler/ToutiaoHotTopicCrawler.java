package com.autoarticle.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 今日头条热榜抓取器（真实 HTTP，失败自动降级为空列表）。
 * 数据源：今日头条热榜 JSON 接口。
 */
@Component
@RequiredArgsConstructor
public class ToutiaoHotTopicCrawler extends HttpJsonTopicCrawler {

    private final ObjectMapper objectMapper;

    @Override
    public String source() {
        return "toutiao";
    }

    @Override
    protected String requestUrl() {
        return "https://www.toutiao.com/hot-event/hot-board/?origin=toutiao_pc";
    }

    @Override
    protected List<CrawledTopic> parse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.path("data");
        List<CrawledTopic> result = new ArrayList<>();
        if (!data.isArray()) {
            return result;
        }
        int rank = 0;
        for (JsonNode item : data) {
            JsonNode title = item.path("Title");
            if (title.isMissingNode() || title.asText().isBlank()) {
                continue;
            }
            rank++;
            result.add(CrawledTopic.builder()
                    .title(title.asText().trim())
                    .source(source())
                    .rank(rank)
                    .hotLevel(levelForRank(rank))
                    .sourceUrl(item.path("Url").asText(""))
                    .build());
        }
        return result;
    }
}
