package com.autoarticle.service;

import com.autoarticle.entity.PlatformAccount;
import com.autoarticle.exception.ResourceNotFoundException;
import com.autoarticle.repository.PlatformAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatformAccountServiceTest {

    @Mock
    private PlatformAccountRepository platformAccountRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PlatformAccountService platformAccountService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(platformAccountService, "encryptionKey", "test-secret-key1234");
    }

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

    @Test
    void should_encrypt_credentials_storage() {
        when(platformAccountRepository.save(any())).thenAnswer(inv -> {
            PlatformAccount a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        Map<String, String> creds = Map.of("appId", "my-app-id", "appSecret", "my-secret-value");
        platformAccountService.createAccount("Test", "gzh", creds);

        verify(platformAccountRepository).save(argThat(a -> {
            String stored = a.getCredentials();
            return stored != null
                    && !stored.contains("appId=my-app-id")
                    && !stored.contains("\"appId\":\"my-app-id\"");
        }));
    }

    @Test
    void should_decrypt_credentials_on_detail() {
        PlatformAccount account = PlatformAccount.builder()
                .id(1L)
                .name("Test")
                .platform("gzh")
                .status("verified")
                .enabled(true)
                .build();
        Map<String, String> creds = Map.of("appId", "my-app-id", "appSecret", "my-secret-value");

        PlatformAccountService spyService = spy(platformAccountService);
        // Store encrypted credentials through public create flow first
        when(platformAccountRepository.save(any())).thenAnswer(inv -> {
            PlatformAccount a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });
        spyService.createAccount("Test", "gzh", creds);

        // Now simulate detail read with the encrypted value
        verify(platformAccountRepository).save(argThat(a -> {
            account.setCredentials(a.getCredentials());
            return true;
        }));

        when(platformAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        var dto = spyService.getAccountById(1L);

        assertEquals("my-app-id", dto.getCredentials().get("appId"));
        assertEquals("my-secret-value", dto.getCredentials().get("appSecret"));
    }

    @Test
    void should_delete_account() {
        when(platformAccountRepository.existsById(1L)).thenReturn(true);

        platformAccountService.deleteAccount(1L);

        verify(platformAccountRepository).deleteById(1L);
    }

    @Test
    void should_throw_when_delete_account_not_found() {
        when(platformAccountRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> platformAccountService.deleteAccount(999L));
    }
}
