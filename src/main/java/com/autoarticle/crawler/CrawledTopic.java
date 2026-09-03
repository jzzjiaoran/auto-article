package com.autoarticle.crawler;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrawledTopic {

    private String title;
    private String source;
    private Integer rank;
    private String hotLevel;
    private String sourceUrl;
}
