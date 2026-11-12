package com.aitravel.reseller.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SocialTokenCipher {
    private static final int NONCE_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.social-token-encryption-key:}")
    private String encodedKey;

    public String encrypt(String value) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] packed = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, packed, 0, nonce.length);
            System.arraycopy(encrypted, 0, packed, nonce.length, encrypted.length);
            return Base64.getEncoder().encodeToString(packed);
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt social provider token", e);
        }
    }

    public String decrypt(String value) {
        try {
            byte[] packed = Base64.getDecoder().decode(value);
            byte[] nonce = java.util.Arrays.copyOfRange(packed, 0, NONCE_LENGTH);
            byte[] encrypted = java.util.Arrays.copyOfRange(packed, NONCE_LENGTH, packed.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not decrypt social provider token", e);
        }
    }

    private SecretKeySpec key() {
        try {
            byte[] key = Base64.getDecoder().decode(encodedKey);
            if (key.length != 32) throw new IllegalArgumentException("Key must contain 32 bytes");
            return new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("SOCIAL_TOKEN_ENCRYPTION_KEY must be Base64 for 32 random bytes", e);
        }
    }
}
