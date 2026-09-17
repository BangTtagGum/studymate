package io.studymate.webhook;

import io.studymate.config.StudyMateProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * GitHub 의 X-Hub-Signature-256 헤더(HMAC-SHA256) 검증.
 * secret 이 비어 있으면 검증을 건너뛴다 (로컬 개발용).
 */
@Component
public class WebhookSignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    private final String secret;

    public WebhookSignatureVerifier(StudyMateProperties properties) {
        this.secret = properties.github().webhookSecret();
    }

    public boolean enabled() {
        return secret != null && !secret.isBlank();
    }

    public boolean verify(byte[] body, String signatureHeader) {
        if (!enabled()) return true;
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX)) return false;

        String expected = PREFIX + HexFormat.of().formatHex(hmac(body));
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signatureHeader.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] hmac(byte[] body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(body);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 계산 실패", e);
        }
    }
}
