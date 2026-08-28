package com.kellidavis.codereviewassistant.github.review;

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
