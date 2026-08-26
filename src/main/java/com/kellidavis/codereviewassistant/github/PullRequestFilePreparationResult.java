package com.kellidavis.codereviewassistant.github;

import java.util.List;

public record PullRequestFilePreparationResult(
        List<PreparedPullRequestFile> preparedFiles,
        int totalChangedFiles,
        int skippedFiles
) {
    public PullRequestFilePreparationResult {
        preparedFiles = List.copyOf(preparedFiles);
    }
}
