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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublishServiceTest {

    @Mock
    private PublishRecordRepository publishRecordRepository;
    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private PlatformAccountRepository platformAccountRepository;
    @Mock
    private AsyncPublishService asyncPublishService;

    @InjectMocks
    private PublishService publishService;

    @Test
    void should_throw_when_article_not_found_on_publish() {
        PublishRequest request = PublishRequest.builder()
                .articleId(999L)
                .accountIds(List.of(1L))
                .build();
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> publishService.publish(request));
    }

    @Test
    void should_throw_when_account_not_found_on_publish() {
        Article article = Article.builder().id(1L).title("Article").build();
        PublishRequest request = PublishRequest.builder()
                .articleId(1L)
                .accountIds(List.of(999L))
                .build();
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(platformAccountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> publishService.publish(request));
    }

    @Test
    void should_create_publish_record_for_verified_account() {
        Article article = Article.builder().id(1L).title("Test Article").build();
        PlatformAccount account = PlatformAccount.builder().id(1L).name("GZH").platform("gzh").build();
        PublishRequest request = PublishRequest.builder()
                .articleId(1L)
                .accountIds(List.of(1L))
                .build();
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(platformAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(publishRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        publishService.publish(request);

        verify(publishRecordRepository, times(1)).save(any());
        verify(asyncPublishService, times(1)).doPublish(any());
    }

    @Test
    void should_create_scheduled_publish_record() {
        Article article = Article.builder().id(1L).title("Test Article").build();
        PlatformAccount account = PlatformAccount.builder().id(1L).name("GZH").platform("gzh").build();
        PublishRequest request = PublishRequest.builder()
                .articleId(1L)
                .accountIds(List.of(1L))
                .scheduledAt(java.time.LocalDateTime.now().plusHours(1))
                .build();
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(platformAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(publishRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        publishService.publish(request);

        verify(publishRecordRepository, times(1)).save(any());
        verify(asyncPublishService, never()).doPublish(any());
    }

    @Test
    void should_return_records_page() {
        Article article = Article.builder().id(1L).title("Article").build();
        PlatformAccount account = PlatformAccount.builder().id(1L).name("GZH").platform("gzh").build();
        PublishRecord record = PublishRecord.builder()
                .id(1L)
                .article(article)
                .account(account)
                .status("pending")
                .retryCount(0)
                .build();
        when(publishRecordRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        var result = publishService.getRecords(0, 10);

        assertEquals(1, result.getTotalElements());
        PublishRecordDto dto = result.getContent().get(0);
        assertEquals("Article", dto.getArticleTitle());
        assertEquals("GZH", dto.getAccountName());
    }

    @Test
    void should_retry_publish() {
        PublishRecord record = PublishRecord.builder()
                .id(1L)
                .status("failed")
                .retryCount(1)
                .build();
        when(publishRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(publishRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        publishService.retryPublish(1L);

        assertEquals("publishing", record.getStatus());
        assertEquals(2, record.getRetryCount());
        verify(asyncPublishService).doPublish(1L);
    }
}
