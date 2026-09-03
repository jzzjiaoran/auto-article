package com.autoarticle.service;

import com.autoarticle.dto.PlatformAccountDto;
import com.autoarticle.entity.PlatformAccount;
import com.autoarticle.exception.ResourceNotFoundException;
import com.autoarticle.repository.PlatformAccountRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformAccountService {

    private final PlatformAccountRepository platformAccountRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.credentials.secret:auto-article-default-secret-key}")
    private String encryptionKey;

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
                .credentials(encryptCredentials(credentials))
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
            Map<String, String> existing = decryptCredentials(account.getCredentials());
            Map<String, String> merged = new java.util.HashMap<>(existing);
            credentials.forEach((k, v) -> {
                if (v != null && !v.isBlank()) {
                    merged.put(k, v);
                }
            });
            account.setCredentials(encryptCredentials(merged));
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
        dto.setCredentials(decryptCredentials(account.getCredentials()));
        return dto;
    }

    private String encryptCredentials(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        try {
            String json = objectMapper.writeValueAsString(map);
            return encrypt(json);
        } catch (Exception e) {
            log.error("Failed to encrypt credentials", e);
            return "";
        }
    }

    private Map<String, String> decryptCredentials(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return Map.of();
        }
        try {
            String json = decrypt(encrypted);
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.error("Failed to decrypt credentials, attempting legacy format", e);
            return parseLegacyCredentials(encrypted);
        }
    }

    private Map<String, String> parseLegacyCredentials(String credentials) {
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

    private String encrypt(String plainText) throws Exception {
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes16 = new byte[16];
        System.arraycopy(keyBytes, 0, keyBytes16, 0, Math.min(keyBytes.length, 16));
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes16, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decrypt(String cipherText) throws Exception {
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes16 = new byte[16];
        System.arraycopy(keyBytes, 0, keyBytes16, 0, Math.min(keyBytes.length, 16));
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes16, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
