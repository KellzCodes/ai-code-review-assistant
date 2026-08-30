package com.kellidavis.codereviewassistant.github.webhook;

import com.kellidavis.codereviewassistant.github.api.GitHubApiException;
import com.kellidavis.codereviewassistant.github.api.GitHubPullRequestCommentResponse;
import com.kellidavis.codereviewassistant.github.api.GitHubPullRequestCommentsClient;
import com.kellidavis.codereviewassistant.github.api.GitHubPullRequestFilesClient;
import com.kellidavis.codereviewassistant.github.review.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;

@Service
public class GitHubWebhookService {
    private static final String PULL_REQUEST_EVENT_TYPE = "pull_request";
    private static final String IGNORED_STATUS = "IGNORED";
    private static final String ACCEPTED_STATUS = "ACCEPTED";

    private static final Set<String> REVIEW_ACTIONS = Set.of(
            "opened",
            "reopened",
            "synchronize");

    private final GitHubPullRequestFilesClient gitHubPullRequestFilesClient;
    private final GitHubPullRequestFilesPreparer gitHubPullRequestFilesPreparer;
    private final GitHubPullRequestPatchExtractor gitHubPullRequestPatchExtractor;
    private final GitHubPullRequestReviewer gitHubPullRequestReviewer;
    private final GitHubPullRequestCommentsClient gitHubPullRequestCommentsClient;
    private final GitHubPullRequestReviewCommentFormatter gitHubPullRequestReviewCommentFormatter;

    public GitHubWebhookService(
            GitHubPullRequestFilesClient gitHubPullRequestFilesClient,
            GitHubPullRequestFilesPreparer gitHubPullRequestFilesPreparer,
            GitHubPullRequestPatchExtractor gitHubPullRequestPatchExtractor,
            GitHubPullRequestReviewer gitHubPullRequestReviewer,
            GitHubPullRequestCommentsClient gitHubPullRequestCommentsClient,
            GitHubPullRequestReviewCommentFormatter gitHubPullRequestReviewCommentFormatter
    ) {
        this.gitHubPullRequestFilesClient = gitHubPullRequestFilesClient;
        this.gitHubPullRequestFilesPreparer = gitHubPullRequestFilesPreparer;
        this.gitHubPullRequestPatchExtractor = gitHubPullRequestPatchExtractor;
        this.gitHubPullRequestReviewer = gitHubPullRequestReviewer;
        this.gitHubPullRequestCommentsClient = gitHubPullRequestCommentsClient;
        this.gitHubPullRequestReviewCommentFormatter = gitHubPullRequestReviewCommentFormatter;
    }

    public GitHubWebhookResponse handle(String eventType, String deliveryId, GitHubPullRequestEvent event) {
        if (!PULL_REQUEST_EVENT_TYPE.equals(eventType)) {
            return new GitHubWebhookResponse(
                    IGNORED_STATUS,
                    deliveryId,
                    eventType,
                    event.action(),
                    event.repository().fullName(),
                    event.number(),
                    0,
                    0,
                    0,
                    0,
                    0,
                    false,
                    null,
                    List.of(),
                    "Webhook event type is not supported.");
        }

        if (!REVIEW_ACTIONS.contains(event.action())) {
            return new GitHubWebhookResponse(
                    IGNORED_STATUS,
                    deliveryId,
                    eventType,
                    event.action(),
                    event.repository().fullName(),
                    event.number(),
                    0,
                    0,
                    0,
                    0,
                    0,
                    false,
                    null,
                    List.of(),
                    "Pull request action does not require a code review.");
        }

        PullRequestFilePreparationResult preparationResult =
                gitHubPullRequestFilesPreparer.prepareFiles(
                        gitHubPullRequestFilesClient.fetchPullRequestFiles(
                                event.repository().fullName(),
                                event.number()));

        PullRequestPatchExtractionResult extractionResult =
                gitHubPullRequestPatchExtractor.extractReviewableFiles(preparationResult.preparedFiles());

        GitHubPullRequestReviewResult reviewResult =
                gitHubPullRequestReviewer.reviewFiles(extractionResult.reviewableFiles());

        String summaryComment = gitHubPullRequestReviewCommentFormatter.format(
                event,
                preparationResult,
                reviewResult);

        boolean summaryCommentPosted = false;
        String summaryCommentUrl = null;
        String summaryCommentFailureMessage = null;

        try {
            GitHubPullRequestCommentResponse commentResponse =
                    gitHubPullRequestCommentsClient.postPullRequestComment(
                            event.repository().fullName(),
                            event.number(),
                            summaryComment);

            summaryCommentPosted = true;
            summaryCommentUrl = commentResponse.htmlUrl();
        } catch (GitHubApiException ex) {
            summaryCommentFailureMessage = ex.getMessage();
        }

        return new GitHubWebhookResponse(
                ACCEPTED_STATUS,
                deliveryId,
                eventType,
                event.action(),
                event.repository().fullName(),
                event.number(),
                preparationResult.totalChangedFiles(),
                preparationResult.preparedFiles().size(),
                preparationResult.skippedFiles(),
                reviewResult.reviewedFiles(),
                reviewResult.totalFindings(),
                summaryCommentPosted,
                summaryCommentUrl,
                reviewResult.findings(),
                buildAcceptedMessage(preparationResult, reviewResult, summaryCommentPosted, summaryCommentFailureMessage));
    }

    private String buildAcceptedMessage(
            PullRequestFilePreparationResult preparationResult,
            GitHubPullRequestReviewResult reviewResult,
            boolean summaryCommentPosted,
            String summaryCommentFailureMessage
    ) {
        String message = "Pull request event accepted and "
                + preparationResult.preparedFiles().size()
                + " file(s) were prepared from "
                + preparationResult.totalChangedFiles()
                + " changed file(s). "
                + preparationResult.skippedFiles()
                + " file(s) were skipped. "
                + reviewResult.reviewedFiles()
                + " file(s) were analyzed and "
                + reviewResult.totalFindings()
                + " review finding(s) were generated.";

        if (summaryCommentPosted) {
            return message + " A summary comment was posted to the pull request.";
        }

        return message
                + " The summary comment could not be posted to the pull request: "
                + summaryCommentFailureMessage;
    }
}
