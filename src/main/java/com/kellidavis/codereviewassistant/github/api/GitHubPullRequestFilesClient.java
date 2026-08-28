package com.kellidavis.codereviewassistant.github.api;

import java.util.List;

public interface GitHubPullRequestFilesClient {

    List<GitHubPullRequestFileResponse> fetchPullRequestFiles(
            String repositoryFullName,
            int pullRequestNumber
    );
}
