package com.kellidavis.codereviewassistant.github.api;

public interface GitHubPullRequestReviewCommentsClient {
    GitHubPullRequestReviewCommentResponse postReviewComment(
            String repositoryFullName,
            int pullRequestNumber,
            String commitSha,
            String filePath,
            int lineNumber,
            String commentBody);
}
