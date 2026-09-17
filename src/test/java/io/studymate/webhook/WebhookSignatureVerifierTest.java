package io.studymate.webhook;

import io.studymate.config.StudyMateProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureVerifierTest {

    private static WebhookSignatureVerifier verifier(String secret) {
        return new WebhookSignatureVerifier(new StudyMateProperties(
                new StudyMateProperties.Github("o", "r", "main", "t", secret, null, null, null),
                new StudyMateProperties.Claude(null, null),
                new StudyMateProperties.Study(null, null, null, 0)));
    }

    @Test
    void accepts_valid_signature() {
        // GitHub 문서 예시: secret "It's a Secret to Everybody", body "Hello, World!"
        byte[] body = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        String sig = "sha256=757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17";

        assertThat(verifier("It's a Secret to Everybody").verify(body, sig)).isTrue();
    }

    @Test
    void rejects_wrong_or_missing_signature() {
        byte[] body = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        WebhookSignatureVerifier v = verifier("It's a Secret to Everybody");

        assertThat(v.verify(body, "sha256=deadbeef")).isFalse();
        assertThat(v.verify(body, null)).isFalse();
    }

    @Test
    void skips_verification_when_secret_is_blank() {
        assertThat(verifier("").verify(new byte[0], null)).isTrue();
    }
}
