package com.kellidavis.codereviewassistant.github;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class GitHubPullRequestFilesPreparerTest {
    private final GitHubPullRequestFilesPreparer gitHubPullRequestFilesPreparer =
            new GitHubPullRequestFilesPreparer(new PullRequestFileLanguageResolver());

    @Test
    void prepareFiles_withReviewableAndSkippedFiles_returnsPreparedFilesAndCounts() {
        List<GitHubPullRequestFileResponse> changedFiles = Arrays.asList(
                new GitHubPullRequestFileResponse(
                        "src/main/java/PaymentService.java",
                        "modified",
                        "@@ -1,2 +1,3 @@",
                        2,
                        1,
                        3,
                        null),
                new GitHubPullRequestFileResponse(
                        "web/src/components/ReviewPanel.tsx",
                        "renamed",
                        "@@ -10,4 +10,5 @@",
                        5,
                        2,
                        7,
                        "web/src/components/LegacyReviewPanel.tsx"),
                new GitHubPullRequestFileResponse(
                        "docs/legacy-notes.txt",
                        "REMOVED",
                        null,
                        0,
                        12,
                        12,
                        null),
                new GitHubPullRequestFileResponse(
                        "assets/logo.png",
                        "modified",
                        null,
                        0,
                        0,
                        0,
                        null),
                new GitHubPullRequestFileResponse(
                        "README.md",
                        "modified",
                        "   ",
                        1,
                        0,
                        1,
                        null),
                null);

        PullRequestFilePreparationResult result = gitHubPullRequestFilesPreparer.prepareFiles(changedFiles);

        assertThat(result.totalChangedFiles()).isEqualTo(6);
        assertThat(result.skippedFiles()).isEqualTo(4);
        assertThat(result.preparedFiles()).hasSize(2);

        PreparedPullRequestFile firstPreparedFile = result.preparedFiles().get(0);
        assertThat(firstPreparedFile.filePath()).isEqualTo("src/main/java/PaymentService.java");
        assertThat(firstPreparedFile.language()).isEqualTo("Java");
        assertThat(firstPreparedFile.changeStatus()).isEqualTo("modified");
        assertThat(firstPreparedFile.patch()).isEqualTo("@@ -1,2 +1,3 @@");
        assertThat(firstPreparedFile.additions()).isEqualTo(2);
        assertThat(firstPreparedFile.deletions()).isEqualTo(1);

        PreparedPullRequestFile secondPreparedFile = result.preparedFiles().get(1);
        assertThat(secondPreparedFile.filePath()).isEqualTo("web/src/components/ReviewPanel.tsx");
        assertThat(secondPreparedFile.language()).isEqualTo("TypeScript");
        assertThat(secondPreparedFile.changeStatus()).isEqualTo("renamed");
        assertThat(secondPreparedFile.patch()).isEqualTo("@@ -10,4 +10,5 @@");
        assertThat(secondPreparedFile.additions()).isEqualTo(5);
        assertThat(secondPreparedFile.deletions()).isEqualTo(2);
    }

    @Test
    void prepareFiles_withNullChangedFiles_returnsEmptyResult() {
        PullRequestFilePreparationResult result = gitHubPullRequestFilesPreparer.prepareFiles(null);

        assertThat(result.totalChangedFiles()).isZero();
        assertThat(result.skippedFiles()).isZero();
        assertThat(result.preparedFiles()).isEmpty();
    }
}
