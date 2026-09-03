package com.autoarticle.crawler;

import com.autoarticle.entity.HotTopic;

import java.util.List;

/**
 * 网络热点抓取器。每个实现抓取一个平台（微博/知乎/头条…）的热点榜单，
 * 字段映射到 {@link HotTopic} 的 title / source / rank / hotLevel / sourceUrl，status 默认 unused。
 */
public interface TopicCrawler {

    /** 来源标识：weibo / zhihu / toutiao … */
    String source();

    /**
     * 抓取热点榜单。实现方负责对内容做基础清洗（去除标签/脚本、trim），
     * 失败时应记录日志并返回空列表（由采集编排逐源隔离异常，不影响其它源）。
     */
    List<HotTopic> fetch();
}
