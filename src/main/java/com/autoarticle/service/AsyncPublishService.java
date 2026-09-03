package com.autoarticle.service;

import com.autoarticle.entity.PublishRecord;
import com.autoarticle.repository.PublishRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncPublishService {

    private final PublishRecordRepository publishRecordRepository;

    @Async
    public void doPublish(Long recordId) {
        PublishRecord record = publishRecordRepository.findById(recordId)
                .orElse(null);
        if (record == null) return;

        try {
            // TODO: replace with real platform publish adapter
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
}
