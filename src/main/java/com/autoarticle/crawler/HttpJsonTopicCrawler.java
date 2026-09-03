package com.autoarticle.crawler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 抓取公共基类：负责携带 UA 头、超时控制与请求/降级兜底。
 * 子类只需实现 source()、请求 URL 与 JSON 解析；任何网络/解析异常都会被捕获并降级为空列表。
 */
@Slf4j
public abstract class HttpJsonTopicCrawler implements TopicCrawler {

    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final RestTemplate restTemplate = buildRestTemplate();

    @Override
    public List<CrawledTopic> fetch() {
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

    protected abstract List<CrawledTopic> parse(String body) throws Exception;

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

    private RestTemplate buildRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(8));
        factory.setReadTimeout((int) TimeUnit.SECONDS.toMillis(8));
        return new RestTemplate(factory);
    }
}
