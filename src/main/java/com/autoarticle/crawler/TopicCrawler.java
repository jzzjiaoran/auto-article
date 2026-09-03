package com.autoarticle.crawler;

import java.util.List;

/**
 * 网络热点抓取器接口。每个实现负责抓取一个平台的热点榜单，
 * 返回规范化后的热点列表。抓取失败时实现应记录日志并返回空列表（降级），
 * 由上层聚合采集逻辑决定是否回退到示例数据。
 */
public interface TopicCrawler {

    /**
     * 来源标识：weibo / zhihu / toutiao / sample
     */
    String source();

    /**
     * 抓取该平台的热点榜单。
     *
     * @return 规范化后的热点列表；失败时返回空列表而非抛出异常
     */
    List<CrawledTopic> fetch();
}
