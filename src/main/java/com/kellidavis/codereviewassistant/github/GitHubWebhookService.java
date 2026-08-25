package com.kellidavis.codereviewassistant.github;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class GitHubWebhookService {

    private static final String PULL_REQUEST_EVENT_TYPE = "pull_request";

    private static final Set<String> REVIEW_ACTIONS = Set.of(
            "opened",
            "reopened",
            "synchronize"
    );

    private final GitHubPullRequestFilesClient gitHubPullRequestFilesClient;

    public GitHubWebhookService(GitHubPullRequestFilesClient gitHubPullRequestFilesClient) {
        this.gitHubPullRequestFilesClient = gitHubPullRequestFilesClient;
    }

    public GitHubWebhookResponse handle(String eventType, String deliveryId, GitHubPullRequestEvent event) {
        if (!PULL_REQUEST_EVENT_TYPE.equals(eventType)) {
            return new GitHubWebhookResponse(
                    "IGNORED",
                    deliveryId,
                    eventType,
                    event.action(),
                    event.repository().fullName(),
                    event.number(),
                    "Webhook event type is not supported."
            );
        }

        if (!REVIEW_ACTIONS.contains(event.action())) {
            return new GitHubWebhookResponse(
                    "IGNORED",
                    deliveryId,
                    eventType,
                    event.action(),
                    event.repository().fullName(),
                    event.number(),
                    "Pull request action does not require a code review."
            );
        }

        int changedFileCount = gitHubPullRequestFilesClient.fetchPullRequestFiles(event.repository().fullName(),
                event.number()).size();

        return new GitHubWebhookResponse(
                "ACCEPTED",
                deliveryId,
                eventType,
                event.action(),
                event.repository().fullName(),
                event.number(),
                "Pull request event accepted and "
                        + changedFileCount
                        + " changed file(s) were retrieved from GitHub."
        );
    }
}