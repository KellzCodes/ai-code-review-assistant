package com.kellidavis.codereviewassistant.github;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GitHubWebhookServiceTest {

    private final GitHubPullRequestFilesClient gitHubPullRequestFilesClient = mock(GitHubPullRequestFilesClient.class);

    private final GitHubWebhookService gitHubWebhookService = new GitHubWebhookService(gitHubPullRequestFilesClient);

    @Test
    void handle_withOpenedPullRequest_returnsAcceptedResponseAndFetchesFiles() {
        GitHubPullRequestEvent event = createEvent("opened");

        List<GitHubPullRequestFileResponse> changedFiles = List.of(
                new GitHubPullRequestFileResponse(
                        "src/main/java/PaymentService.java",
                        "modified",
                        "@@ -1,4 +1,5 @@",
                        3,
                        1,
                        4,
                        null
                ),
                new GitHubPullRequestFileResponse(
                        "src/main/java/OrderService.java",
                        "added",
                        "@@ -0,0 +1,8 @@",
                        8,
                        0,
                        8,
                        null
                )
        );

        when(gitHubPullRequestFilesClient.fetchPullRequestFiles("kellidavis/ai-code-review-assistant",
                42)).thenReturn(changedFiles);

        GitHubWebhookResponse response = gitHubWebhookService.handle("pull_request", "delivery-123",
                event);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.deliveryId()).isEqualTo("delivery-123");
        assertThat(response.eventType()).isEqualTo("pull_request");
        assertThat(response.action()).isEqualTo("opened");
        assertThat(response.repository()).isEqualTo("kellidavis/ai-code-review-assistant");
        assertThat(response.pullRequestNumber()).isEqualTo(42);
        assertThat(response.message()).isEqualTo(
                        "Pull request event accepted and 2 changed file(s) were retrieved from GitHub.");

        verify(gitHubPullRequestFilesClient).fetchPullRequestFiles("kellidavis/ai-code-review-assistant",
                42);
    }

    @Test
    void handle_withClosedPullRequest_returnsIgnoredResponse() {
        GitHubPullRequestEvent event = createEvent("closed");

        GitHubWebhookResponse response = gitHubWebhookService.handle("pull_request", "delivery-456",
                event);

        assertThat(response.status()).isEqualTo("IGNORED");
        assertThat(response.action()).isEqualTo("closed");
        assertThat(response.message()).isEqualTo("Pull request action does not require a code review.");

        verifyNoInteractions(gitHubPullRequestFilesClient);
    }

    @Test
    void handle_withUnsupportedEventType_returnsIgnoredResponse() {
        GitHubPullRequestEvent event = createEvent("opened");

        GitHubWebhookResponse response = gitHubWebhookService.handle("push", "delivery-789", event);

        assertThat(response.status()).isEqualTo("IGNORED");
        assertThat(response.eventType()).isEqualTo("push");
        assertThat(response.message()).isEqualTo("Webhook event type is not supported.");

        verifyNoInteractions(gitHubPullRequestFilesClient);
    }

    private GitHubPullRequestEvent createEvent(String action) {
        GitHubPullRequest pullRequest = new GitHubPullRequest("Add payment validation",
                "https://github.com/kellidavis/ai-code-review-assistant/pull/42");

        GitHubRepository repository = new GitHubRepository("kellidavis/ai-code-review-assistant");

        return new GitHubPullRequestEvent(action, 42, pullRequest, repository);
    }
}