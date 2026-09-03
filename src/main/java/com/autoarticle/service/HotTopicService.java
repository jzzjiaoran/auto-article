package com.autoarticle.service;

import com.autoarticle.dto.HotTopicDto;
import com.autoarticle.entity.HotTopic;
import com.autoarticle.exception.ResourceNotFoundException;
import com.autoarticle.repository.HotTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotTopicService {

    private final HotTopicRepository hotTopicRepository;

    public Page<HotTopicDto> getTopics(String keyword, String source, String status, int page, int size) {
        return hotTopicRepository.findByFilters(keyword, source, status, PageRequest.of(page, size))
                .map(this::toDto);
    }

    public HotTopicDto getTopicById(Long id) {
        HotTopic topic = hotTopicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("热点", id));
        return toDetailDto(topic);
    }

    @Transactional
    public void refreshTopic(Long id) {
        HotTopic topic = hotTopicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("热点", id));
        log.info("Refreshing hot topic: {}", topic.getTitle());
        topic.setStatus("unused");
        hotTopicRepository.save(topic);
    }

    private HotTopicDto toDto(HotTopic topic) {
        long articleCount = topic.getArticles() != null ? topic.getArticles().size() : 0;
        return HotTopicDto.builder()
                .id(topic.getId())
                .title(topic.getTitle())
                .source(topic.getSource())
                .rank(topic.getRank())
                .hotLevel(topic.getHotLevel())
                .status(topic.getStatus())
                .sourceUrl(topic.getSourceUrl())
                .collectedAt(topic.getCollectedAt())
                .articleCount(articleCount)
                .build();
    }

    private HotTopicDto toDetailDto(HotTopic topic) {
        HotTopicDto dto = toDto(topic);
        dto.setSourceUrl(topic.getSourceUrl());
        return dto;
    }
}
