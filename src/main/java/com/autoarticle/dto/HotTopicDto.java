package com.autoarticle.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotTopicDto {

    private Long id;
    private String title;
    private String source;
    private Integer rank;
    private String hotLevel;
    private String status;
    private String sourceUrl;
    private LocalDateTime collectedAt;
    private Long articleCount;
}
