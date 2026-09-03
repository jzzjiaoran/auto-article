package com.autoarticle.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformAccountDto {

    private Long id;
    private String name;
    private String platform;
    private String status;
    private Boolean enabled;
    private LocalDateTime lastVerifyAt;
    private Map<String, String> credentials;
}
