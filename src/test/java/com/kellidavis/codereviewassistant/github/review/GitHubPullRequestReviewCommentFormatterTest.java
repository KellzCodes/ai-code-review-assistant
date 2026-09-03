package com.kellidavis.codereviewassistant.github.review;

import com.kellidavis.codereviewassistant.github.webhook.GitHubPullRequest;
import com.kellidavis.codereviewassistant.github.webhook.GitHubPullRequestEvent;
import com.kellidavis.codereviewassistant.github.webhook.GitHubPullRequestHead;
import com.kellidavis.codereviewassistant.github.webhook.GitHubRepository;
import com.kellidavis.codereviewassistant.review.ReviewCategory;
import com.kellidavis.codereviewassistant.review.ReviewFinding;
import com.kellidavis.codereviewassistant.review.ReviewSeverity;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class GitHubPullRequestReviewCommentFormatterTest {
    private final GitHubPullRequestReviewCommentFormatter formatter =
            new GitHubPullRequestReviewCommentFormatter();

    @Test
    void format_withFindings_returnsMarkdownSummary() {
        GitHubPullRequestEvent event = createEvent();
        PullRequestFilePreparationResult preparationResult =
                new PullRequestFilePreparationResult(
                        List.of(
                                new PreparedPullRequestFile(
                                        "src/main/java/PaymentService.java",
                                        "Java",
                                        "modified",
                                        "@@ -1,1 +1,2 @@",
                                        1,
                                        0),
                                new PreparedPullRequestFile(
                                        "src/main/java/OrderService.java",
                                        "Java",
                                        "added",
                                        "@@ -0,0 +1,3 @@",
                                        3,
                                        0)),
                        3,
                        1);
        GitHubPullRequestReviewResult reviewResult =
                new GitHubPullRequestReviewResult(
                        List.of(
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
                                        "Possible hardcoded secret detected. Store sensitive values in environment variables or a secret manager.")),
                        2);

        String comment = formatter.format(event, preparationResult, reviewResult);

        assertThat(comment).isEqualTo("""
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

                _Generated automatically by the AI Code Review Assistant._""");
    }

    @Test
    void format_withoutReviewedFiles_returnsNoReviewableCodeMessage() {
        GitHubPullRequestEvent event = createEvent();
        PullRequestFilePreparationResult preparationResult =
                new PullRequestFilePreparationResult(List.of(), 2, 2);
        GitHubPullRequestReviewResult reviewResult =
                new GitHubPullRequestReviewResult(List.of(), 0);

        String comment = formatter.format(event, preparationResult, reviewResult);

        assertThat(comment).isEqualTo("""
                <!-- ai-code-review-assistant-summary -->
                ## AI Code Review Summary

                Pull request: [#42 Add payment validation](https://github.com/kellidavis/ai-code-review-assistant/pull/42)
                Repository: `kellidavis/ai-code-review-assistant`

                Changed files: 2
                Prepared files: 0
                Skipped files: 2
                Reviewed files: 0
                Total findings: 0

                ### Result

                No reviewable added lines were available to analyze in this pull request.

                _Generated automatically by the AI Code Review Assistant._""");
    }

    @Test
    void format_withReviewedFilesAndNoFindings_returnsCleanSummaryMessage() {
        GitHubPullRequestEvent event = createEvent();
        PullRequestFilePreparationResult preparationResult =
                new PullRequestFilePreparationResult(
                        List.of(
                                new PreparedPullRequestFile(
                                        "src/main/java/PaymentService.java",
                                        "Java",
                                        "modified",
                                        "@@ -1,1 +1,2 @@",
                                        1,
                                        0)),
                        1,
                        0);
        GitHubPullRequestReviewResult reviewResult =
                new GitHubPullRequestReviewResult(List.of(), 1);

        String comment = formatter.format(event, preparationResult, reviewResult);

        assertThat(comment).isEqualTo("""
                <!-- ai-code-review-assistant-summary -->
                ## AI Code Review Summary

                Pull request: [#42 Add payment validation](https://github.com/kellidavis/ai-code-review-assistant/pull/42)
                Repository: `kellidavis/ai-code-review-assistant`

                Changed files: 1
                Prepared files: 1
                Skipped files: 0
                Reviewed files: 1
                Total findings: 0

                ### Result

                No review findings were generated from the analyzed pull request changes.

                _Generated automatically by the AI Code Review Assistant._""");
    }

    @Test
    void isSummaryComment_withMarker_returnsTrue() {
        assertThat(formatter.isSummaryComment("""
                <!-- ai-code-review-assistant-summary -->
                ## AI Code Review Summary
                """)).isTrue();
    }

    @Test
    void isSummaryComment_withoutMarker_returnsFalse() {
        assertThat(formatter.isSummaryComment("## AI Code Review Summary")).isFalse();
    }

    @Test
    void formatInlineReviewComment_withFinding_returnsMarkdownComment() {
        ReviewFinding finding = new ReviewFinding(
                "src/main/java/PaymentService.java",
                14,
                ReviewCategory.MAINTAINABILITY,
                ReviewSeverity.LOW,
                "Avoid System.out.println in application code. Use a logger instead.");

        String comment = formatter.formatInlineReviewComment(finding);

        assertThat(comment).isEqualTo("""
                <!-- ai-code-review-assistant-inline -->
                **[LOW] MAINTAINABILITY**

                Avoid System.out.println in application code. Use a logger instead.

                _Generated automatically by the AI Code Review Assistant._""");
    }

    private GitHubPullRequestEvent createEvent() {
        return new GitHubPullRequestEvent(
                "opened",
                42,
                new GitHubPullRequest(
                        "Add payment validation",
                        "https://github.com/kellidavis/ai-code-review-assistant/pull/42",
                        new GitHubPullRequestHead("abc123def456")),
                new GitHubRepository("kellidavis/ai-code-review-assistant"));
    }
}
