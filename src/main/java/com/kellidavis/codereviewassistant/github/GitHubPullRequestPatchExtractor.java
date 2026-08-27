package com.kellidavis.codereviewassistant.github;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class GitHubPullRequestPatchExtractor {
    public PullRequestPatchExtractionResult extractReviewableFiles(List<PreparedPullRequestFile> preparedFiles) {
        if (preparedFiles == null) {
            return new PullRequestPatchExtractionResult(List.of(), 0, 0);
        }

        List<ReviewablePullRequestFile> reviewableFiles = new ArrayList<>();
        int skippedFiles = 0;

        for (PreparedPullRequestFile preparedFile : preparedFiles) {
            String reviewableCode = extractReviewableCode(preparedFile);
            if (reviewableCode == null) {
                skippedFiles++;
                continue;
            }

            reviewableFiles.add(new ReviewablePullRequestFile(
                    preparedFile.filePath(),
                    preparedFile.language(),
                    reviewableCode));
        }

        return new PullRequestPatchExtractionResult(reviewableFiles, preparedFiles.size(), skippedFiles);
    }

    private String extractReviewableCode(PreparedPullRequestFile preparedFile) {
        if (preparedFile == null || preparedFile.patch() == null || preparedFile.patch().isBlank()) {
            return null;
        }

        List<String> addedLines = new ArrayList<>();
        boolean insideHunk = false;

        for (String patchLine : preparedFile.patch().lines().toList()) {
            if (isHunkHeaderLine(patchLine)) {
                insideHunk = true;
                continue;
            }

            if (!insideHunk) {
                continue;
            }

            if (isPostHunkMetadataLine(patchLine) || isRemovedLine(patchLine) || isContextLine(patchLine)) {
                continue;
            }

            if (patchLine.startsWith("+")) {
                addedLines.add(patchLine.substring(1));
            }
        }

        if (addedLines.isEmpty()) {
            return null;
        }

        String reviewableCode = String.join("\n", addedLines);
        return reviewableCode.isBlank() ? null : reviewableCode;
    }

    private boolean isHunkHeaderLine(String patchLine) {
        return patchLine.startsWith("@@");
    }

    private boolean isPostHunkMetadataLine(String patchLine) {
        return patchLine.startsWith("\\ No newline at end of file");
    }

    private boolean isRemovedLine(String patchLine) {
        return patchLine.startsWith("-");
    }

    private boolean isContextLine(String patchLine) {
        return patchLine.startsWith(" ");
    }
}
