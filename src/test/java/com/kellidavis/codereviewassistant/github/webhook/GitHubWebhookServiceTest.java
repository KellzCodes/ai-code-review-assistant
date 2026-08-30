package com.kellidavis.codereviewassistant.github.webhook;

import com.kellidavis.codereviewassistant.github.api.GitHubApiException;
import com.kellidavis.codereviewassistant.github.api.GitHubPullRequestCommentResponse;
import com.kellidavis.codereviewassistant.github.api.GitHubPullRequestCommentsClient;
import com.kellidavis.codereviewassistant.github.api.GitHubPullRequestFileResponse;
import com.kellidavis.codereviewassistant.github.api.GitHubPullRequestFilesClient;
import com.kellidavis.codereviewassistant.github.review.GitHubPullRequestFilesPreparer;
import com.kellidavis.codereviewassistant.github.review.GitHubPullRequestPatchExtractor;
import com.kellidavis.codereviewassistant.github.review.GitHubPullRequestReviewCommentFormatter;
import com.kellidavis.codereviewassistant.github.review.GitHubPullRequestReviewer;
import com.kellidavis.codereviewassistant.github.review.PullRequestFileLanguageResolver;
import com.kellidavis.codereviewassistant.review.ReviewCategory;
import com.kellidavis.codereviewassistant.review.ReviewFinding;
import com.kellidavis.codereviewassistant.review.ReviewSeverity;
import com.kellidavis.codereviewassistant.review.analysis.RuleBasedCodeAnalyzer;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GitHubWebhookServiceTest {
    private final GitHubPullRequestFilesClient gitHubPullRequestFilesClient = mock(GitHubPullRequestFilesClient.class);
    private final GitHubPullRequestCommentsClient gitHubPullRequestCommentsClient =
            mock(GitHubPullRequestCommentsClient.class);
    private final PullRequestFileLanguageResolver pullRequestFileLanguageResolver = new PullRequestFileLanguageResolver();
    private final GitHubPullRequestFilesPreparer gitHubPullRequestFilesPreparer =
            new GitHubPullRequestFilesPreparer(pullRequestFileLanguageResolver);
    private final GitHubPullRequestPatchExtractor gitHubPullRequestPatchExtractor =
            new GitHubPullRequestPatchExtractor();
    private final GitHubPullRequestReviewer gitHubPullRequestReviewer =
            new GitHubPullRequestReviewer(new RuleBasedCodeAnalyzer());
    private final GitHubPullRequestReviewCommentFormatter gitHubPullRequestReviewCommentFormatter =
            new GitHubPullRequestReviewCommentFormatter();

    private final GitHubWebhookService gitHubWebhookService = new GitHubWebhookService(
            gitHubPullRequestFilesClient,
            gitHubPullRequestFilesPreparer,
            gitHubPullRequestPatchExtractor,
            gitHubPullRequestReviewer,
            gitHubPullRequestCommentsClient,
            gitHubPullRequestReviewCommentFormatter);

    @Test
    void handle_withOpenedPullRequest_returnsAcceptedResponseAndPostsSummaryComment() {
        GitHubPullRequestEvent event = createEvent("opened");

        List<GitHubPullRequestFileResponse> changedFiles = List.of(
                new GitHubPullRequestFileResponse(
                        "src/main/java/PaymentService.java",
                        "modified",
                        """
                        @@ -1,4 +1,5 @@
                         public class PaymentService {
                        +    System.out.println("Processing payment");
                         }
                        """,
                        3,
                        1,
                        4,
                        null),
                new GitHubPullRequestFileResponse(
                        "src/main/java/OrderService.java",
                        "added",
                        """
                        @@ -0,0 +1,3 @@
                        +public class OrderService {
                        +    String token = "secret123";
                        +}
                        """,
                        8,
                        0,
                        8,
                        null),
                new GitHubPullRequestFileResponse(
                        "docs/legacy-notes.txt",
                        "removed",
                        null,
                        0,
                        12,
                        12,
                        null));

        when(gitHubPullRequestFilesClient.fetchPullRequestFiles("kellidavis/ai-code-review-assistant",
                42)).thenReturn(changedFiles);
        when(gitHubPullRequestCommentsClient.postPullRequestComment(
                "kellidavis/ai-code-review-assistant",
                42,
                """
                <!-- ai-code-review-assistant-summary -->
                ## AI Code Review Summary

                Pull request: [#42 Add payment validation](https://github.com/kellidavis/ai-code-review-assistant/pull/42)
                Repository: `kellidavis/ai-code-review-assistant`

                Changed files: 3
                Prepared files: 2
                Skipped files: 1
                Reviewed files: 2
                Total findings: 2

                ### Findings

                1. **[HIGH] SECURITY** `src/main/java/OrderService.java:2`
                   Possible hardcoded secret detected. Store sensitive values in environment variables or a secret manager.
                2. **[LOW] MAINTAINABILITY** `src/main/java/PaymentService.java:2`
                   Avoid System.out.println in application code. Use a logger instead.

                _Generated automatically by the AI Code Review Assistant._"""))
                .thenReturn(new GitHubPullRequestCommentResponse(
                "https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1"));

        GitHubWebhookResponse response = gitHubWebhookService.handle("pull_request", "delivery-123",
                event);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.deliveryId()).isEqualTo("delivery-123");
        assertThat(response.eventType()).isEqualTo("pull_request");
        assertThat(response.action()).isEqualTo("opened");
        assertThat(response.repository()).isEqualTo("kellidavis/ai-code-review-assistant");
        assertThat(response.pullRequestNumber()).isEqualTo(42);
        assertThat(response.totalChangedFiles()).isEqualTo(3);
        assertThat(response.preparedFiles()).isEqualTo(2);
        assertThat(response.skippedFiles()).isEqualTo(1);
        assertThat(response.reviewedFiles()).isEqualTo(2);
        assertThat(response.totalFindings()).isEqualTo(2);
        assertThat(response.summaryCommentPosted()).isTrue();
        assertThat(response.summaryCommentUrl())
                .isEqualTo("https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1");
        assertThat(response.findings()).containsExactly(
                new ReviewFinding(
                        "src/main/java/PaymentService.java",
                        2,
                        ReviewCategory.MAINTAINABILITY,
                        ReviewSeverity.LOW,
                        "Avoid System.out.println in application code. Use a logger instead."),
                new ReviewFinding(
                        "src/main/java/OrderService.java",
                        2,
                        ReviewCategory.SECURITY,
                        ReviewSeverity.HIGH,
                        "Possible hardcoded secret detected. Store sensitive values in environment variables or a secret manager."));
        assertThat(response.message()).isEqualTo(
                "Pull request event accepted and 2 file(s) were prepared from 3 changed file(s). 1 file(s) were skipped. 2 file(s) were analyzed and 2 review finding(s) were generated. A summary comment was posted to the pull request.");

        verify(gitHubPullRequestFilesClient).fetchPullRequestFiles("kellidavis/ai-code-review-assistant",
                42);
        verify(gitHubPullRequestCommentsClient).postPullRequestComment(
                eq("kellidavis/ai-code-review-assistant"),
                eq(42),
                contains("## AI Code Review Summary"));
    }

    @Test
    void handle_whenSummaryCommentPostFails_returnsAcceptedResponseWithFailureDetails() {
        GitHubPullRequestEvent event = createEvent("opened");

        List<GitHubPullRequestFileResponse> changedFiles = List.of(
                new GitHubPullRequestFileResponse(
                        "src/main/java/PaymentService.java",
                        "modified",
                        """
                        @@ -1,4 +1,5 @@
                         public class PaymentService {
                        +    System.out.println("Processing payment");
                         }
                        """,
                        3,
                        1,
                        4,
                        null));

        when(gitHubPullRequestFilesClient.fetchPullRequestFiles(
                "kellidavis/ai-code-review-assistant",
                42)).thenReturn(changedFiles);
        when(gitHubPullRequestCommentsClient.postPullRequestComment(
                eq("kellidavis/ai-code-review-assistant"),
                eq(42),
                contains("## AI Code Review Summary"))).thenThrow(new GitHubApiException(
                        "GitHub API returned 403 while posting a pull request comment for kellidavis/ai-code-review-assistant#42."));

        GitHubWebhookResponse response = gitHubWebhookService.handle("pull_request", "delivery-123", event);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.summaryCommentPosted()).isFalse();
        assertThat(response.summaryCommentUrl()).isNull();
        assertThat(response.totalFindings()).isEqualTo(1);
        assertThat(response.message())
                .isEqualTo("Pull request event accepted and 1 file(s) were prepared from 1 changed file(s). 0 file(s) were skipped. 1 file(s) were analyzed and 1 review finding(s) were generated. The summary comment could not be posted to the pull request: GitHub API returned 403 while posting a pull request comment for kellidavis/ai-code-review-assistant#42.");
    }

    @Test
    void handle_withClosedPullRequest_returnsIgnoredResponse() {
        GitHubPullRequestEvent event = createEvent("closed");

        GitHubWebhookResponse response = gitHubWebhookService.handle("pull_request", "delivery-456",
                event);

        assertThat(response.status()).isEqualTo("IGNORED");
        assertThat(response.action()).isEqualTo("closed");
        assertThat(response.totalChangedFiles()).isZero();
        assertThat(response.preparedFiles()).isZero();
        assertThat(response.skippedFiles()).isZero();
        assertThat(response.reviewedFiles()).isZero();
        assertThat(response.totalFindings()).isZero();
        assertThat(response.summaryCommentPosted()).isFalse();
        assertThat(response.summaryCommentUrl()).isNull();
        assertThat(response.findings()).isEmpty();
        assertThat(response.message()).isEqualTo("Pull request action does not require a code review.");

        verifyNoInteractions(gitHubPullRequestFilesClient, gitHubPullRequestCommentsClient);
    }

    @Test
    void handle_withUnsupportedEventType_returnsIgnoredResponse() {
        GitHubPullRequestEvent event = createEvent("opened");

        GitHubWebhookResponse response = gitHubWebhookService.handle("push", "delivery-789", event);

        assertThat(response.status()).isEqualTo("IGNORED");
        assertThat(response.eventType()).isEqualTo("push");
        assertThat(response.totalChangedFiles()).isZero();
        assertThat(response.preparedFiles()).isZero();
        assertThat(response.skippedFiles()).isZero();
        assertThat(response.reviewedFiles()).isZero();
        assertThat(response.totalFindings()).isZero();
        assertThat(response.summaryCommentPosted()).isFalse();
        assertThat(response.summaryCommentUrl()).isNull();
        assertThat(response.findings()).isEmpty();
        assertThat(response.message()).isEqualTo("Webhook event type is not supported.");

        verifyNoInteractions(gitHubPullRequestFilesClient, gitHubPullRequestCommentsClient);
    }

    private GitHubPullRequestEvent createEvent(String action) {
        GitHubPullRequest pullRequest = new GitHubPullRequest("Add payment validation",
                "https://github.com/kellidavis/ai-code-review-assistant/pull/42");

        GitHubRepository repository = new GitHubRepository("kellidavis/ai-code-review-assistant");

        return new GitHubPullRequestEvent(action, 42, pullRequest, repository);
    }
}
