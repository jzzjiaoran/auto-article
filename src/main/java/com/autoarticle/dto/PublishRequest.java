package com.autoarticle.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishRequest {

    @NotNull(message = "请选择文章")
    private Long articleId;

    @NotEmpty(message = "请选择发布平台")
    private List<Long> accountIds;

    private LocalDateTime scheduledAt;
}
