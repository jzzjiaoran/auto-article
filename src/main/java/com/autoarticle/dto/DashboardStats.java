package com.autoarticle.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStats {

    private Long articleCount;
    private Long hotTopicCount;
    private Long accountCount;
    private Long publishCount;
}
