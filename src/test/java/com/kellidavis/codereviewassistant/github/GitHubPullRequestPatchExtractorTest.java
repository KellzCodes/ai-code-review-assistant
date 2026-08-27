package com.kellidavis.codereviewassistant.github;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class GitHubPullRequestPatchExtractorTest {
    private final GitHubPullRequestPatchExtractor gitHubPullRequestPatchExtractor =
            new GitHubPullRequestPatchExtractor();

    @Test
    void extractReviewableFiles_withMixedPatchContent_returnsOnlyAddedCode() {
        List<PreparedPullRequestFile> preparedFiles = Arrays.asList(
                new PreparedPullRequestFile(
                        "src/main/java/PaymentService.java",
                        "Java",
                        "modified",
                        """
                        diff --git a/src/main/java/PaymentService.java b/src/main/java/PaymentService.java
                        index 1234567..89abcde 100644
                        --- a/src/main/java/PaymentService.java
                        +++ b/src/main/java/PaymentService.java
                        @@ -8,3 +8,4 @@ public class PaymentService {
                         public void process() {
                        -    return;
                        +    logger.info("processed");
                        +    return;
                         }
                        """,
                        2,
                        1),
                new PreparedPullRequestFile(
                        "web/src/components/ReviewPanel.tsx",
                        "TypeScript",
                        "added",
                        """
                        @@ -0,0 +1,4 @@
                        +const token = "abc123";
                        +console.log(token);
                        +
                        +export default token;
                        """,
                        4,
                        0));

        PullRequestPatchExtractionResult result = gitHubPullRequestPatchExtractor.extractReviewableFiles(preparedFiles);

        assertThat(result.totalPreparedFiles()).isEqualTo(2);
        assertThat(result.skippedFiles()).isZero();
        assertThat(result.reviewableFiles()).hasSize(2);

        ReviewablePullRequestFile firstReviewableFile = result.reviewableFiles().get(0);
        assertThat(firstReviewableFile.filePath()).isEqualTo("src/main/java/PaymentService.java");
        assertThat(firstReviewableFile.language()).isEqualTo("Java");
        assertThat(firstReviewableFile.reviewableCode())
                .isEqualTo("    logger.info(\"processed\");\n    return;");

        ReviewablePullRequestFile secondReviewableFile = result.reviewableFiles().get(1);
        assertThat(secondReviewableFile.filePath()).isEqualTo("web/src/components/ReviewPanel.tsx");
        assertThat(secondReviewableFile.language()).isEqualTo("TypeScript");
        assertThat(secondReviewableFile.reviewableCode())
                .isEqualTo("const token = \"abc123\";\nconsole.log(token);\n\nexport default token;");
    }

    @Test
    void extractReviewableFiles_withFilesThatStillHaveNoReviewableCode_skipsThem() {
        List<PreparedPullRequestFile> preparedFiles = Arrays.asList(
                new PreparedPullRequestFile(
                        "README.md",
                        "Markdown",
                        "modified",
                        """
                        @@ -1,2 +1,2 @@
                        -Old heading
                        +   
                        """,
                        1,
                        1
                ),
                new PreparedPullRequestFile(
                        "src/main/java/LegacyService.java",
                        "Java",
                        "modified",
                        """
                        @@ -10,2 +10,0 @@
                        -    String token = "old-secret";
                        -    return token;
                        """,
                        0,
                        2),
                null);

        PullRequestPatchExtractionResult result = gitHubPullRequestPatchExtractor.extractReviewableFiles(preparedFiles);

        assertThat(result.totalPreparedFiles()).isEqualTo(3);
        assertThat(result.skippedFiles()).isEqualTo(3);
        assertThat(result.reviewableFiles()).isEmpty();
    }

    @Test
    void extractReviewableFiles_withAddedLineThatStartsWithTriplePlus_preservesCodeLine() {
        List<PreparedPullRequestFile> preparedFiles = List.of(
                new PreparedPullRequestFile(
                        "src/main/java/CounterService.java",
                        "Java",
                        "modified",
                        """
                        diff --git a/src/main/java/CounterService.java b/src/main/java/CounterService.java
                        index 1234567..89abcde 100644
                        --- a/src/main/java/CounterService.java
                        +++ b/src/main/java/CounterService.java
                        @@ -4,1 +4,1 @@
                        +++counter;
                        """,
                        1,
                        0
                )
        );

        PullRequestPatchExtractionResult result =
                gitHubPullRequestPatchExtractor.extractReviewableFiles(preparedFiles);

        assertThat(result.totalPreparedFiles()).isEqualTo(1);
        assertThat(result.skippedFiles()).isZero();
        assertThat(result.reviewableFiles()).hasSize(1);
        assertThat(result.reviewableFiles().get(0).reviewableCode()).isEqualTo("++counter;");
    }

    @Test
    void extractReviewableFiles_withNullPreparedFiles_returnsEmptyResult() {
        PullRequestPatchExtractionResult result =
                gitHubPullRequestPatchExtractor.extractReviewableFiles(null);

        assertThat(result.totalPreparedFiles()).isZero();
        assertThat(result.skippedFiles()).isZero();
        assertThat(result.reviewableFiles()).isEmpty();
    }
}
