package com.autoarticle.crawler;

import com.autoarticle.entity.HotTopic;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 抓取公共基类：携带 UA 头、超时控制，解析失败/网络失败时降级为空列表。
 * 子类只需实现 source()、请求 URL 与 JSON 解析。
 */
@Slf4j
public abstract class HttpJsonTopicCrawler implements TopicCrawler {

    private static final int TIMEOUT_SECONDS = 8;

    private final RestTemplate restTemplate = buildRestTemplate();

    @Override
    public List<HotTopic> fetch() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, buildUserAgent());
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            ResponseEntity<String> response = restTemplate.exchange(
                    requestUrl(), HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("[crawler:{}] unexpected http status: {}", source(), response.getStatusCode());
                return List.of();
            }
            return parse(response.getBody());
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("[crawler:{}] fetch failed, degraded to empty list: {}", source(), e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.warn("[crawler:{}] unexpected error, degraded to empty list: {}", source(), e.getMessage());
            return List.of();
        }
    }

    protected abstract String requestUrl();

    protected abstract List<HotTopic> parse(String body) throws Exception;

    protected static String levelForRank(int rank) {
        if (rank <= 5) {
            return "high";
        }
        if (rank <= 15) {
            return "middle";
        }
        return "normal";
    }

    protected String buildUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/120.0 Safari/537.36";
    }

    protected String clean(String raw) {
        if (raw == null) {
            return "";
        }
        return Jsoup.parse(raw).text().trim();
    }

    private RestTemplate buildRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        factory.setReadTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        return new RestTemplate(factory);
    }
}
