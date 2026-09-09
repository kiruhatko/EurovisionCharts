package com.eurovision.analytics.security;

import com.eurovision.analytics.config.SecurityProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * AES-256-GCM encryption at rest for OAuth access/refresh tokens, keyed
 * exclusively from {@code TOKEN_ENCRYPTION_KEY} (never persisted, never
 * logged). Every connected-account token column is written and read
 * exclusively through this service.
 */
@Service
public class TokenEncryptionService {

    private final SecurityProperties properties;
    private BytesEncryptor encryptor;

    public TokenEncryptionService(SecurityProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        if (!properties.isConfigured()) {
            // No key configured: token encryption is unavailable, matching every
            // provider integration's "not configured" fail-closed behavior. Any
            // attempt to encrypt/decrypt will throw rather than silently store
            // plaintext.
            this.encryptor = null;
            return;
        }
        String key = properties.tokenEncryptionKey();
        String salt = HexFormat.of().formatHex(sha256(key.getBytes(StandardCharsets.UTF_8))).substring(0, 32);
        this.encryptor = Encryptors.stronger(key, salt);
    }

    public boolean isConfigured() {
        return encryptor != null;
    }

    public byte[] encrypt(String plaintext) {
        requireConfigured();
        if (plaintext == null) {
            return null;
        }
        return encryptor.encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public String decrypt(byte[] ciphertext) {
        requireConfigured();
        if (ciphertext == null) {
            return null;
        }
        return new String(encryptor.decrypt(ciphertext), StandardCharsets.UTF_8);
    }

    private void requireConfigured() {
        if (encryptor == null) {
            throw new IllegalStateException("TOKEN_ENCRYPTION_KEY is not configured; refusing to handle OAuth tokens");
        }
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is a mandatory JDK algorithm", e);
        }
    }
}
