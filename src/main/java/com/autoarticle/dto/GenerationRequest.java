package com.autoarticle.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationRequest {

    private Long topicId;

    @NotBlank(message = "文章标题不能为空")
    private String title;

    private String style;

    private String length;

    private String prompt;

    private Boolean saveDraft;
}
