package com.autoarticle.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskStatus {

    private String taskId;
    private String status;
    private String message;
    private Long articleId;
}
