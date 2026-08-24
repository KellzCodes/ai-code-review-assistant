package com.kellidavis.codereviewassistant.github;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;

class GitHubWebhookSignatureVerifierTest {
    private static final String SECRET = "It's a Secret to Everybody";
    private static final String VALID_SIGNATURE = "sha256=757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17";

    private final GitHubWebhookSignatureVerifier verifier = new GitHubWebhookSignatureVerifier(SECRET);

    @Test
    void isValid_withCorrectSignature_returnsTrue(){
        byte[] payload = "Hello, World!".getBytes(StandardCharsets.UTF_8);

        boolean valid = verifier.isValid(payload, VALID_SIGNATURE);

        assertThat(valid).isTrue();
    }

    @Test
    void isValid_withModifiedPayload_returnsFalse(){
        byte[] payload = "Modified payload".getBytes(StandardCharsets.UTF_8);

        boolean valid = verifier.isValid(payload, VALID_SIGNATURE);

        assertThat(valid).isFalse();
    }

    @Test
    void isValid_withMissingSignature_returnsFalse() {
        byte[] payload = "Hello, World!".getBytes(
                StandardCharsets.UTF_8
        );

        boolean valid = verifier.isValid(payload, null);

        assertThat(valid).isFalse();
    }

    @Test
    void isValid_withMissingSecret_returnsFalse() {
        GitHubWebhookSignatureVerifier verifierWithoutSecret = new GitHubWebhookSignatureVerifier("");

        byte[] payload = "Hello, World!".getBytes(StandardCharsets.UTF_8);

        boolean valid = verifierWithoutSecret.isValid(payload, VALID_SIGNATURE);

        assertThat(valid).isFalse();
    }
}
