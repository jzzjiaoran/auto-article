package com.autoarticle.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleDto {

    private Long id;
    private String title;
    private String content;
    private String contentHtml;
    private String summary;
    private String status;
    private Integer wordCount;
    private String aiProvider;
    private String aiModel;
    private String style;
    private String length;
    private Long hotTopicId;
    private String hotTopicTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
