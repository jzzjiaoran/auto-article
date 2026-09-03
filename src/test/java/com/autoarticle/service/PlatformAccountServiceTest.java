package com.autoarticle.service;

import com.autoarticle.entity.Article;
import com.autoarticle.entity.PlatformAccount;
import com.autoarticle.exception.ResourceNotFoundException;
import com.autoarticle.repository.PlatformAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatformAccountServiceTest {

    @Mock
    private PlatformAccountRepository platformAccountRepository;

    @InjectMocks
    private PlatformAccountService platformAccountService;

    @Test
    void should_throw_when_account_not_found() {
        when(platformAccountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> platformAccountService.getAccountById(999L));
    }

    @Test
    void should_create_account() {
        when(platformAccountRepository.save(any())).thenAnswer(inv -> {
            PlatformAccount a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        var result = platformAccountService.createAccount("Test GZH", "gzh", Map.of("appId", "123"));

        assertEquals("Test GZH", result.getName());
        assertEquals("unverified", result.getStatus());
    }

    @Test
    void should_verify_account() {
        PlatformAccount account = PlatformAccount.builder()
                .id(1L)
                .name("Test")
                .status("unverified")
                .build();
        when(platformAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(platformAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        platformAccountService.verifyAccount(1L);

        assertEquals("verified", account.getStatus());
        assertNotNull(account.getLastVerifyAt());
    }
}
