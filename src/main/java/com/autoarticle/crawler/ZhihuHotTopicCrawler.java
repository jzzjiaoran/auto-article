package com.autoarticle.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 知乎热榜抓取器（真实 HTTP，失败自动降级为空列表）。
 * 数据源：知乎热榜 JSON 接口。
 */
@Component
@RequiredArgsConstructor
public class ZhihuHotTopicCrawler extends HttpJsonTopicCrawler {

    private final ObjectMapper objectMapper;

    @Override
    public String source() {
        return "zhihu";
    }

    @Override
    protected String requestUrl() {
        return "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=50";
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
            JsonNode target = item.path("target");
            JsonNode title = target.path("title");
            if (title.isMissingNode() || title.asText().isBlank()) {
                continue;
            }
            rank++;
            result.add(CrawledTopic.builder()
                    .title(title.asText().trim())
                    .source(source())
                    .rank(rank)
                    .hotLevel(levelForRank(rank))
                    .sourceUrl(buildDetailUrl(target))
                    .build());
        }
        return result;
    }

    private String buildDetailUrl(JsonNode target) {
        String type = target.path("type").asText();
        String id = target.path("id").asText();
        if (id == null || id.isBlank()) {
            return "https://www.zhihu.com/hot";
        }
        if ("question".equals(type)) {
            return "https://www.zhihu.com/question/" + id;
        }
        if ("article".equals(type)) {
            return "https://zhuanlan.zhihu.com/p/" + id;
        }
        return "https://www.zhihu.com/hot";
    }
}
