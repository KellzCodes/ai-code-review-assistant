package com.kellidavis.codereviewassistant.github;

import java.util.List;

public record PullRequestPatchExtractionResult(
        List<ReviewablePullRequestFile> reviewableFiles,
        int totalPreparedFiles,
        int skippedFiles
) {
    public PullRequestPatchExtractionResult {
        reviewableFiles = List.copyOf(reviewableFiles);
    }
}
