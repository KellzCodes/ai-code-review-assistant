package com.kellidavis.codereviewassistant.github.webhook;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubWebhookPayloadParserTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private final GitHubWebhookPayloadParser payloadParser =
            new GitHubWebhookPayloadParser(
                    JsonMapper.builder().build(),
                    validator);

    @Test
    void parse_withValidPayload_returnsEvent() {
        byte[] payload = validPayload().getBytes(StandardCharsets.UTF_8);

        GitHubPullRequestEvent event = payloadParser.parse(payload);

        assertThat(event.action()).isEqualTo("opened");

        assertThat(event.number()).isEqualTo(42);

        assertThat(event.pullRequest().title()).isEqualTo("Add payment validation");

        assertThat(event.pullRequest().htmlUrl())
                .isEqualTo("https://github.com/kellidavis/ai-code-review-assistant/pull/42");

        assertThat(event.pullRequest().head().sha()).isEqualTo("abc123def456");

        assertThat(event.repository().fullName()).isEqualTo("kellidavis/ai-code-review-assistant");
    }

    @Test
    void parse_withBlankAction_throwsConstraintViolation() {
        String payload = validPayload().replace("\"action\": \"opened\"", "\"action\": \"\"");

        assertThatThrownBy(() -> payloadParser.parse(payload.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void parse_withBlankPullRequestHeadSha_throwsConstraintViolation() {
        String payload = validPayload().replace("\"sha\": \"abc123def456\"", "\"sha\": \"\"");

        assertThatThrownBy(() -> payloadParser.parse(payload.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void parse_withMalformedJson_throwsPayloadException() {
        byte[] payload = "{not valid JSON}".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> payloadParser.parse(payload))
                .isInstanceOf(InvalidGitHubWebhookPayloadException.class).hasMessage(
                        "Webhook payload must contain valid JSON.").hasCauseInstanceOf(JacksonException.class);
    }

    private String validPayload() {
        return """
                {
                  "action": "opened",
                  "number": 42,
                  "pull_request": {
                    "title": "Add payment validation",
                    "html_url": "https://github.com/kellidavis/ai-code-review-assistant/pull/42",
                    "head": {
                      "sha": "abc123def456"
                    }
                  },
                  "repository": {
                    "full_name": "kellidavis/ai-code-review-assistant"
                  }
                }
                """;
    }
}
