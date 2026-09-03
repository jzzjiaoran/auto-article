package com.autoarticle.service;

import com.autoarticle.dto.PlatformAccountDto;
import com.autoarticle.entity.PlatformAccount;
import com.autoarticle.exception.BusinessException;
import com.autoarticle.exception.ResourceNotFoundException;
import com.autoarticle.repository.PlatformAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformAccountService {

    private final PlatformAccountRepository platformAccountRepository;

    public List<PlatformAccountDto> getAllAccounts() {
        return platformAccountRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<PlatformAccountDto> getVerifiedAccounts() {
        return platformAccountRepository.findByStatus("verified").stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public PlatformAccountDto getAccountById(Long id) {
        PlatformAccount account = platformAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("平台账号", id));
        return toDetailDto(account);
    }

    @Transactional
    public PlatformAccountDto createAccount(String name, String platform, Map<String, String> credentials) {
        PlatformAccount account = PlatformAccount.builder()
                .name(name)
                .platform(platform)
                .status("unverified")
                .credentials(mapToString(credentials))
                .enabled(true)
                .build();
        account = platformAccountRepository.save(account);
        log.info("Created platform account: {} for {}", name, platform);
        return toDto(account);
    }

    @Transactional
    public PlatformAccountDto updateAccount(Long id, String name, String platform,
                                            Map<String, String> credentials, Boolean enabled) {
        PlatformAccount account = platformAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("平台账号", id));
        account.setName(name);
        if (credentials != null) {
            account.setCredentials(mapToString(credentials));
        }
        if (enabled != null) {
            account.setEnabled(enabled);
        }
        account = platformAccountRepository.save(account);
        log.info("Updated platform account: {}", account.getName());
        return toDto(account);
    }

    @Transactional
    public void verifyAccount(Long id) {
        PlatformAccount account = platformAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("平台账号", id));
        account.setStatus("verified");
        account.setLastVerifyAt(LocalDateTime.now());
        platformAccountRepository.save(account);
        log.info("Verified platform account: {}", account.getName());
    }

    @Transactional
    public void deleteAccount(Long id) {
        if (!platformAccountRepository.existsById(id)) {
            throw new ResourceNotFoundException("平台账号", id);
        }
        platformAccountRepository.deleteById(id);
        log.info("Deleted platform account: {}", id);
    }

    private PlatformAccountDto toDto(PlatformAccount account) {
        return PlatformAccountDto.builder()
                .id(account.getId())
                .name(account.getName())
                .platform(account.getPlatform())
                .status(account.getStatus())
                .enabled(account.getEnabled())
                .lastVerifyAt(account.getLastVerifyAt())
                .build();
    }

    private PlatformAccountDto toDetailDto(PlatformAccount account) {
        PlatformAccountDto dto = toDto(account);
        dto.setCredentials(parseCredentials(account.getCredentials()));
        return dto;
    }

    private Map<String, String> parseCredentials(String credentials) {
        if (credentials == null || credentials.isBlank()) {
            return Map.of();
        }
        Map<String, String> map = new java.util.HashMap<>();
        for (String part : credentials.split("\\|")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }

    private String mapToString(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("|"));
    }
}
