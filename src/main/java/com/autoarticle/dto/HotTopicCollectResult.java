package com.autoarticle.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotTopicCollectResult {

    private int added;
    private int skipped;
    private boolean sampleFallbackUsed;
    private boolean interrupted;

    @Builder.Default
    private List<String> sourceMessages = new ArrayList<>();

    public static HotTopicCollectResult busy() {
        return HotTopicCollectResult.builder()
                .interrupted(true)
                .build();
    }

    public String toMessage() {
        if (interrupted) {
            return "有采集任务正在执行，请稍后再试";
        }
        StringBuilder sb = new StringBuilder("热点采集完成：新增 " + added + " 条");
        if (skipped > 0) {
            sb.append("，跳过重复 ").append(skipped).append(" 条");
        }
        if (sampleFallbackUsed) {
            sb.append("（当前为内置示例数据）");
        }
        for (String m : sourceMessages) {
            sb.append("；").append(m);
        }
        return sb.toString();
    }
}
