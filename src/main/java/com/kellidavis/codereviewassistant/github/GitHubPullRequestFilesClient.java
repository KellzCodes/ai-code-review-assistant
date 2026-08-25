package com.kellidavis.codereviewassistant.github;

import java.util.List;

public interface GitHubPullRequestFilesClient {

    List<GitHubPullRequestFileResponse> fetchPullRequestFiles(
            String repositoryFullName,
            int pullRequestNumber
    );
}
