package com.kellidavis.codereviewassistant.github.api;

import java.util.List;

public interface GitHubPullRequestReviewCommentsClient {
    List<GitHubPullRequestReviewCommentResponse> listPullRequestReviewComments(
            String repositoryFullName,
            int pullRequestNumber);

    GitHubPullRequestReviewCommentResponse postReviewComment(
            String repositoryFullName,
            int pullRequestNumber,
            String commitSha,
            String filePath,
            int lineNumber,
            String commentBody);
}
