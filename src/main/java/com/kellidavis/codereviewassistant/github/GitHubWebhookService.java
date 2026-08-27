package com.kellidavis.codereviewassistant.github;

import org.springframework.stereotype.Service;
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

    public GitHubWebhookService(
            GitHubPullRequestFilesClient gitHubPullRequestFilesClient,
            GitHubPullRequestFilesPreparer gitHubPullRequestFilesPreparer,
            GitHubPullRequestPatchExtractor gitHubPullRequestPatchExtractor,
            GitHubPullRequestReviewer gitHubPullRequestReviewer
    ) {
        this.gitHubPullRequestFilesClient = gitHubPullRequestFilesClient;
        this.gitHubPullRequestFilesPreparer = gitHubPullRequestFilesPreparer;
        this.gitHubPullRequestPatchExtractor = gitHubPullRequestPatchExtractor;
        this.gitHubPullRequestReviewer = gitHubPullRequestReviewer;
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
                "Pull request event accepted and "
                        + preparationResult.preparedFiles().size()
                        + " file(s) were prepared from "
                        + preparationResult.totalChangedFiles()
                        + " changed file(s). "
                        + preparationResult.skippedFiles()
                        + " file(s) were skipped. "
                        + reviewResult.reviewedFiles()
                        + " file(s) were analyzed and "
                        + reviewResult.totalFindings()
                        + " review finding(s) were generated.");
    }
}
