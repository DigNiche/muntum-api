package com.digniche.muntum.auth.social;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SocialTokenCipher {

    private static final String TRANSFORMATION =
            "AES/GCM/NoPadding";

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String VERSION_PREFIX = "v1:";

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom =
            new SecureRandom();

    public SocialTokenCipher(
            @Value("${social.apple.token-encryption-key}")
            String encodedKey
    ) {
        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder()
                    .decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Apple 토큰 암호화 키가 올바른 Base64 형식이 아닙니다",
                    exception
            );
        }

        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "Apple 토큰 암호화 키는 32바이트여야 합니다"
            );
        }

        this.secretKey =
                new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException(
                    "암호화할 토큰이 없습니다"
            );
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher =
                    Cipher.getInstance(TRANSFORMATION);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(
                            TAG_LENGTH_BITS,
                            iv
                    )
            );

            byte[] encrypted = cipher.doFinal(
                    plainText.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            byte[] combined =
                    new byte[iv.length + encrypted.length];

            System.arraycopy(
                    iv,
                    0,
                    combined,
                    0,
                    iv.length
            );

            System.arraycopy(
                    encrypted,
                    0,
                    combined,
                    iv.length,
                    encrypted.length
            );

            return VERSION_PREFIX
                    + Base64.getEncoder()
                    .encodeToString(combined);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "소셜 토큰 암호화에 실패했습니다",
                    exception
            );
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null
                || encryptedText.isBlank()) {
            throw new IllegalArgumentException(
                    "복호화할 토큰이 없습니다"
            );
        }

        if (!encryptedText.startsWith(
                VERSION_PREFIX
        )) {
            throw new IllegalArgumentException(
                    "지원하지 않는 암호화 형식입니다"
            );
        }

        try {
            byte[] combined = Base64.getDecoder()
                    .decode(
                            encryptedText.substring(
                                    VERSION_PREFIX.length()
                            )
                    );

            if (combined.length <= IV_LENGTH) {
                throw new IllegalArgumentException(
                        "암호화된 토큰 형식이 잘못되었습니다"
                );
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted =
                    new byte[combined.length - IV_LENGTH];

            System.arraycopy(
                    combined,
                    0,
                    iv,
                    0,
                    IV_LENGTH
            );

            System.arraycopy(
                    combined,
                    IV_LENGTH,
                    encrypted,
                    0,
                    encrypted.length
            );

            Cipher cipher =
                    Cipher.getInstance(TRANSFORMATION);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(
                            TAG_LENGTH_BITS,
                            iv
                    )
            );

            byte[] decrypted =
                    cipher.doFinal(encrypted);

            return new String(
                    decrypted,
                    StandardCharsets.UTF_8
            );

        } catch (IllegalArgumentException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "소셜 토큰 복호화에 실패했습니다",
                    exception
            );
        }
    }
}