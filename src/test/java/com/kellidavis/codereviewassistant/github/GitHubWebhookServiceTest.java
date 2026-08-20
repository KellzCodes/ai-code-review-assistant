package com.kellidavis.codereviewassistant.github;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubWebhookServiceTest {

    private final GitHubWebhookService gitHubWebhookService =
            new GitHubWebhookService();

    private GitHubPullRequestEvent createEvent(String action){
        GitHubPullRequest pullRequest = new GitHubPullRequest(
                "Add payment validation",
                "https://github.com/kellidavis/ai-code-review-assistant/pull/42"
        );

        GitHubRepository repository = new GitHubRepository(
                "kellidavis/ai-code-review-assistant"
        );

        return new GitHubPullRequestEvent(action, 42, pullRequest, repository);
    }

    @Test
    void handle_withOpenedPullRequest_returnsAcceptedResponse() {
        GitHubPullRequestEvent event = createEvent("opened");

        GitHubWebhookResponse response =
                gitHubWebhookService.handle(
                        "pull_request",
                        "delivery-123",
                        event
                );

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.deliveryId())
                .isEqualTo("delivery-123");
        assertThat(response.eventType())
                .isEqualTo("pull_request");
        assertThat(response.action()).isEqualTo("opened");
        assertThat(response.repository())
                .isEqualTo(
                        "kellidavis/ai-code-review-assistant"
                );
        assertThat(response.pullRequestNumber()).isEqualTo(42);
        assertThat(response.message())
                .isEqualTo(
                        "Pull request event accepted for future review processing."
                );
    }

    @Test
    void handle_withClosedPullRequest_returnsIgnoredResponse(){
        GitHubPullRequestEvent event = createEvent("closed");

        GitHubWebhookResponse response = gitHubWebhookService.handle(
                "pull_request",
                "delivery-456",
                event);

        assertThat(response.status()).isEqualTo("IGNORED");
        assertThat(response.action()).isEqualTo("closed");
        assertThat(response.message()).isEqualTo("Pull request action does not require a code review.");
    }

    @Test
    void handle_withUnsupportedEventType_returnsIgnoredResponse(){
        GitHubPullRequestEvent event = createEvent("opened");

        GitHubWebhookResponse response = gitHubWebhookService.handle("push", "delivery-789", event);

        assertThat(response.status()).isEqualTo("IGNORED");
        assertThat(response.eventType()).isEqualTo("push");
        assertThat(response.message()).isEqualTo("Webhook event type is not supported.");
    }
}