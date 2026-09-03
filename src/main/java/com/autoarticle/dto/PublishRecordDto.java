package com.autoarticle.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishRecordDto {

    private Long id;
    private Long articleId;
    private String articleTitle;
    private Long accountId;
    private String accountName;
    private String accountPlatform;
    private String status;
    private Integer retryCount;
    private LocalDateTime publishedAt;
    private LocalDateTime scheduledAt;
    private String errorMessage;
}
