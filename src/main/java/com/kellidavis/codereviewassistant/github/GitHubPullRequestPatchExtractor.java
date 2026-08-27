package com.kellidavis.codereviewassistant.github;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GitHubPullRequestPatchExtractor {
    private static final Pattern HUNK_HEADER_PATTERN =
            Pattern.compile("^@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@.*$");

    public PullRequestPatchExtractionResult extractReviewableFiles(List<PreparedPullRequestFile> preparedFiles) {
        if (preparedFiles == null) {
            return new PullRequestPatchExtractionResult(List.of(), 0, 0);
        }

        List<ReviewablePullRequestFile> reviewableFiles = new ArrayList<>();
        int skippedFiles = 0;

        for (PreparedPullRequestFile preparedFile : preparedFiles) {
            ReviewablePullRequestFile reviewableFile = extractReviewableFile(preparedFile);
            if (reviewableFile == null) {
                skippedFiles++;
                continue;
            }

            reviewableFiles.add(reviewableFile);
        }

        return new PullRequestPatchExtractionResult(reviewableFiles, preparedFiles.size(), skippedFiles);
    }

    private ReviewablePullRequestFile extractReviewableFile(PreparedPullRequestFile preparedFile) {
        if (preparedFile == null || preparedFile.patch() == null || preparedFile.patch().isBlank()) {
            return null;
        }

        List<String> addedLines = new ArrayList<>();
        List<Integer> fileLineNumbers = new ArrayList<>();
        boolean insideHunk = false;
        int currentFileLineNumber = -1;

        for (String patchLine : preparedFile.patch().lines().toList()) {
            if (isHunkHeaderLine(patchLine)) {
                currentFileLineNumber = extractNewFileStartLineNumber(patchLine);
                if (currentFileLineNumber < 0) {
                    return null;
                }

                insideHunk = true;
                continue;
            }

            if (!insideHunk) {
                continue;
            }

            if (isPostHunkMetadataLine(patchLine)) {
                continue;
            }

            if (isContextLine(patchLine)) {
                currentFileLineNumber++;
                continue;
            }

            if (isRemovedLine(patchLine)) {
                continue;
            }

            if (isAddedLine(patchLine)) {
                addedLines.add(patchLine.substring(1));
                fileLineNumbers.add(currentFileLineNumber);
                currentFileLineNumber++;
            }
        }

        if (addedLines.isEmpty()) {
            return null;
        }

        String reviewableCode = String.join("\n", addedLines);
        if (reviewableCode.isBlank()) {
            return null;
        }

        return new ReviewablePullRequestFile(
                preparedFile.filePath(),
                preparedFile.language(),
                reviewableCode,
                fileLineNumbers);
    }

    private boolean isHunkHeaderLine(String patchLine) {
        return patchLine.startsWith("@@");
    }

    private int extractNewFileStartLineNumber(String patchLine) {
        Matcher matcher = HUNK_HEADER_PATTERN.matcher(patchLine);
        if (!matcher.matches()) {
            return -1;
        }

        return Integer.parseInt(matcher.group(1));
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

    private boolean isAddedLine(String patchLine) {
        return patchLine.startsWith("+");
    }
}
