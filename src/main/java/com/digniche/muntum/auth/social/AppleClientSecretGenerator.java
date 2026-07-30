package com.digniche.muntum.auth.social;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class AppleClientSecretGenerator {

    private static final String APPLE_ISSUER =
            "https://appleid.apple.com";

    private final String clientId;
    private final String teamId;
    private final String keyId;
    private final ECPrivateKey privateKey;

    public AppleClientSecretGenerator(
            @Value("${social.apple.client-id}")
            String clientId,

            @Value("${social.apple.team-id}")
            String teamId,

            @Value("${social.apple.key-id}")
            String keyId,

            @Value("${social.apple.private-key-path}")
            String privateKeyPath
    ) {
        this.clientId = clientId;
        this.teamId = teamId;
        this.keyId = keyId;
        this.privateKey = loadPrivateKey(privateKeyPath);
    }

    public String generate() {
        Instant now = Instant.now();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(teamId)
                .subject(clientId)
                .audience(APPLE_ISSUER)
                .issueTime(Date.from(now))
                .expirationTime(
                        Date.from(now.plusSeconds(300))
                )
                .build();

        JWSHeader header = new JWSHeader.Builder(
                JWSAlgorithm.ES256
        )
                .keyID(keyId)
                .build();

        SignedJWT signedJwt =
                new SignedJWT(header, claims);

        try {
            signedJwt.sign(
                    new ECDSASigner(privateKey)
            );

            return signedJwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException(
                    "Apple client secret 생성 실패",
                    exception
            );
        }
    }

    private ECPrivateKey loadPrivateKey(
            String privateKeyPath
    ) {
        try {
            String pem = Files.readString(
                    Path.of(privateKeyPath)
            );

            String keyContent = pem
                    .replace(
                            "-----BEGIN PRIVATE KEY-----",
                            ""
                    )
                    .replace(
                            "-----END PRIVATE KEY-----",
                            ""
                    )
                    .replaceAll("\\s", "");

            byte[] decoded =
                    Base64.getDecoder()
                            .decode(keyContent);

            PKCS8EncodedKeySpec keySpec =
                    new PKCS8EncodedKeySpec(decoded);

            KeyFactory keyFactory =
                    KeyFactory.getInstance("EC");

            return (ECPrivateKey)
                    keyFactory.generatePrivate(keySpec);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Apple 개인키 파일을 읽을 수 없습니다: "
                            + privateKeyPath,
                    exception
            );
        }
    }
}