package com.autoarticle.crawler;

import com.autoarticle.entity.HotTopic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 知乎热榜抓取器（真实 HTTP，失败自动降级为空列表）。
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
    protected List<HotTopic> parse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.path("data");
        List<HotTopic> result = new ArrayList<>();
        if (!data.isArray()) {
            return result;
        }
        int rank = 0;
        for (JsonNode item : data) {
            JsonNode target = item.path("target");
            String title = clean(target.path("title").asText(""));
            if (title.isBlank()) {
                continue;
            }
            rank++;
            result.add(HotTopic.builder()
                    .title(title)
                    .source(source())
                    .rank(rank)
                    .hotLevel(levelForRank(rank))
                    .sourceUrl(detailUrl(target))
                    .build());
        }
        return result;
    }

    private String detailUrl(JsonNode target) {
        String type = target.path("type").asText("");
        String id = target.path("id").asText("");
        if (id.isBlank()) {
            return "https://www.zhihu.com/hot";
        }
        return switch (type) {
            case "question" -> "https://www.zhihu.com/question/" + id;
            case "article" -> "https://zhuanlan.zhihu.com/p/" + id;
            default -> "https://www.zhihu.com/hot";
        };
    }
}
