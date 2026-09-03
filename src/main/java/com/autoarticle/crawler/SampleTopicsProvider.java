package com.autoarticle.crawler;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 内置示例热点数据源。
 *
 * 真实平台抓取在无外网或平台反爬时会返回空列表；为保证全新环境列表页有数据、主链路可走通，
 * 采集流程可在真实抓取结果为空时回退到此处提供的示例/种子数据（第一阶段方案，见 JZZ-16）。
 */
@Component
public class SampleTopicsProvider {

    public List<CrawledTopic> topics() {
        List<CrawledTopic> list = new ArrayList<>();
        list.add(topic("国内旅游市场持续升温 多地景区迎来客流高峰", "weibo", 1, "high",
                "https://s.weibo.com/weibo?q=%E6%97%85%E6%B8%B8"));
        list.add(topic("新一轮科技革命带动产业升级 人工智能应用加速落地", "weibo", 2, "high",
                "https://s.weibo.com/weibo?q=%E4%BA%BA%E5%B7%A5%E6%99%BA%E8%83%BD"));
        list.add(topic("国产大飞机商业运营稳步推进 民航市场活力增强", "weibo", 3, "high",
                "https://s.weibo.com/weibo?q=%E5%9B%BD%E4%BA%A7%E5%A4%A7%E9%A3%9E%E6%9C%BA"));
        list.add(topic("健康饮食受关注 专家提醒注意营养均衡", "weibo", 4, "middle",
                "https://s.weibo.com/weibo?q=%E5%81%A5%E5%BA%B7%E9%A5%AE%E9%A3%9F"));
        list.add(topic("新能源车下乡政策成效显著 充电设施加速完善", "weibo", 5, "middle",
                "https://s.weibo.com/weibo?q=%E6%96%B0%E8%83%BD%E6%BA%90%E8%BD%A6"));

        list.add(topic("如何评价人工智能大模型对内容创作行业的影响？", "zhihu", 1, "high",
                "https://www.zhihu.com/question/ai-content"));
        list.add(topic("长期坚持运动能带来哪些身体和心理上的改变？", "zhihu", 2, "high",
                "https://www.zhihu.com/question/exercise"));
        list.add(topic("年轻人如何做好个人理财规划？有哪些建议？", "zhihu", 3, "middle",
                "https://www.zhihu.com/question/finance"));
        list.add(topic("有哪些值得推荐的高质量科普书籍？", "zhihu", 4, "middle",
                "https://www.zhihu.com/question/books"));
        list.add(topic("职场新人如何快速适应工作节奏？", "zhihu", 5, "normal",
                "https://www.zhihu.com/question/workplace"));

        list.add(topic("多地发布促消费新政 助力经济稳步复苏", "toutiao", 1, "high",
                "https://www.toutiao.com/hot/consume"));
        list.add(topic("科技创新驱动高质量发展 关键核心技术不断突破", "toutiao", 2, "high",
                "https://www.toutiao.com/hot/tech"));
        list.add(topic("乡村振兴深入推进 特色产业带动农民增收", "toutiao", 3, "middle",
                "https://www.toutiao.com/hot/rural"));
        list.add(topic("全民健身热潮涌动 体育消费市场持续扩容", "toutiao", 4, "middle",
                "https://www.toutiao.com/hot/sports"));
        list.add(topic("暑期出行安全提示：这些细节要注意", "toutiao", 5, "normal",
                "https://www.toutiao.com/hot/safety"));
        return list;
    }

    private CrawledTopic topic(String title, String source, int rank, String hotLevel, String sourceUrl) {
        return CrawledTopic.builder()
                .title(title)
                .source(source)
                .rank(rank)
                .hotLevel(hotLevel)
                .sourceUrl(sourceUrl)
                .build();
    }
}
