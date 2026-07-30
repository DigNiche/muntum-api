package com.digniche.muntum.auth.social;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SocialTokenCipherTest {

    private SocialTokenCipher createCipher() {
        byte[] keyBytes = new byte[32];
        Arrays.fill(keyBytes, (byte) 1);

        String encodedKey = Base64.getEncoder()
                .encodeToString(keyBytes);

        return new SocialTokenCipher(encodedKey);
    }

    @Test
    void refreshToken을_암호화하고_복호화할_수_있다() {
        SocialTokenCipher cipher = createCipher();
        String originalToken = "apple-refresh-token";

        String encryptedToken =
                cipher.encrypt(originalToken);

        String decryptedToken =
                cipher.decrypt(encryptedToken);

        assertNotEquals(originalToken, encryptedToken);
        assertEquals(originalToken, decryptedToken);
    }

    @Test
    void 같은_토큰도_매번_다른_암호문이_생성된다() {
        SocialTokenCipher cipher = createCipher();
        String originalToken = "apple-refresh-token";

        String first =
                cipher.encrypt(originalToken);

        String second =
                cipher.encrypt(originalToken);

        assertNotEquals(first, second);

        assertEquals(
                originalToken,
                cipher.decrypt(first)
        );

        assertEquals(
                originalToken,
                cipher.decrypt(second)
        );
    }
}