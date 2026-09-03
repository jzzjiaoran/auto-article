package com.autoarticle.service;

import com.autoarticle.dto.PublishRecordDto;
import com.autoarticle.dto.PublishRequest;
import com.autoarticle.entity.Article;
import com.autoarticle.entity.PlatformAccount;
import com.autoarticle.entity.PublishRecord;
import com.autoarticle.exception.ResourceNotFoundException;
import com.autoarticle.repository.ArticleRepository;
import com.autoarticle.repository.PlatformAccountRepository;
import com.autoarticle.repository.PublishRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishService {

    private final PublishRecordRepository publishRecordRepository;
    private final ArticleRepository articleRepository;
    private final PlatformAccountRepository platformAccountRepository;

    @Transactional
    public void publish(PublishRequest request) {
        Article article = articleRepository.findById(request.getArticleId())
                .orElseThrow(() -> new ResourceNotFoundException("文章", request.getArticleId()));

        for (Long accountId : request.getAccountIds()) {
            PlatformAccount account = platformAccountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("平台账号", accountId));

            PublishRecord record = PublishRecord.builder()
                    .article(article)
                    .account(account)
                    .status(request.getScheduledAt() != null ? "pending" : "publishing")
                    .scheduledAt(request.getScheduledAt())
                    .build();
            publishRecordRepository.save(record);

            if (request.getScheduledAt() == null) {
                doPublish(record.getId());
            }
        }
        log.info("Publish initiated for article {} to {} platforms", article.getTitle(), request.getAccountIds().size());
    }

    @Async
    public void doPublish(Long recordId) {
        PublishRecord record = publishRecordRepository.findById(recordId)
                .orElse(null);
        if (record == null) return;

        try {
            Thread.sleep(1000);
            record.setStatus("success");
            record.setPublishedAt(LocalDateTime.now());
            log.info("Published record {} successfully", recordId);
        } catch (Exception e) {
            record.setStatus("failed");
            record.setErrorMessage(e.getMessage());
            record.setRetryCount(record.getRetryCount() + 1);
            log.error("Publish failed for record {}", recordId, e);
        }
        publishRecordRepository.save(record);
    }

    @Transactional
    public void retryPublish(Long recordId) {
        PublishRecord record = publishRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("发布记录", recordId));
        record.setStatus("publishing");
        record.setRetryCount(record.getRetryCount() + 1);
        record.setErrorMessage(null);
        publishRecordRepository.save(record);
        doPublish(recordId);
    }

    public Page<PublishRecordDto> getRecords(int page, int size) {
        return publishRecordRepository.findAll(PageRequest.of(page, size))
                .map(this::toDto);
    }

    public List<PublishRecordDto> getRecordsByArticle(Long articleId) {
        return publishRecordRepository.findByArticleIdOrderByCreatedAtDesc(articleId, PageRequest.of(0, 100))
                .getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private PublishRecordDto toDto(PublishRecord record) {
        return PublishRecordDto.builder()
                .id(record.getId())
                .articleId(record.getArticle().getId())
                .articleTitle(record.getArticle().getTitle())
                .accountId(record.getAccount().getId())
                .accountName(record.getAccount().getName())
                .accountPlatform(record.getAccount().getPlatform())
                .status(record.getStatus())
                .retryCount(record.getRetryCount())
                .publishedAt(record.getPublishedAt())
                .scheduledAt(record.getScheduledAt())
                .errorMessage(record.getErrorMessage())
                .build();
    }
}
