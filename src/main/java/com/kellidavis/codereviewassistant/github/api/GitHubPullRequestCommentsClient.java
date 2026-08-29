package com.kellidavis.codereviewassistant.github.api;

public interface GitHubPullRequestCommentsClient {
    GitHubPullRequestCommentResponse postPullRequestComment(
            String repositoryFullName,
            int pullRequestNumber,
            String commentBody);
}
