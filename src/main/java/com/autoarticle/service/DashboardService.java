package com.autoarticle.service;

import com.autoarticle.dto.DashboardStats;
import com.autoarticle.dto.ArticleDto;
import com.autoarticle.repository.ArticleRepository;
import com.autoarticle.repository.HotTopicRepository;
import com.autoarticle.repository.PlatformAccountRepository;
import com.autoarticle.repository.PublishRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ArticleRepository articleRepository;
    private final HotTopicRepository hotTopicRepository;
    private final PlatformAccountRepository platformAccountRepository;
    private final PublishRecordRepository publishRecordRepository;

    public DashboardStats getStats() {
        return DashboardStats.builder()
                .articleCount(articleRepository.count())
                .hotTopicCount(hotTopicRepository.count())
                .accountCount(platformAccountRepository.count())
                .publishCount(publishRecordRepository.count())
                .build();
    }

    public List<ArticleDto> getRecentArticles() {
        return articleRepository.findTop5ByOrderByUpdatedAtDesc().stream()
                .map(a -> ArticleDto.builder()
                        .id(a.getId())
                        .title(a.getTitle())
                        .status(a.getStatus())
                        .wordCount(a.getWordCount())
                        .updatedAt(a.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
