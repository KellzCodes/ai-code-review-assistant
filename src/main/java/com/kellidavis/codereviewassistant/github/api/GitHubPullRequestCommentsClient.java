package com.kellidavis.codereviewassistant.github.api;

import java.util.List;

public interface GitHubPullRequestCommentsClient {

    List<GitHubPullRequestCommentResponse> listPullRequestComments(String repositoryFullName, int pullRequestNumber);

    GitHubPullRequestCommentResponse postPullRequestComment(String repositoryFullName, int pullRequestNumber, String commentBody);

    GitHubPullRequestCommentResponse updatePullRequestComment(String repositoryFullName, long commentId, String commentBody);
}
