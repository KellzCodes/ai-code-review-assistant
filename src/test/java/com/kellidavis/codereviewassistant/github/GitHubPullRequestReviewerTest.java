package com.kellidavis.codereviewassistant.github;

import com.kellidavis.codereviewassistant.review.ReviewCategory;
import com.kellidavis.codereviewassistant.review.ReviewFinding;
import com.kellidavis.codereviewassistant.review.ReviewRequest;
import com.kellidavis.codereviewassistant.review.ReviewSeverity;
import com.kellidavis.codereviewassistant.review.analysis.CodeAnalyzer;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GitHubPullRequestReviewerTest {

    @Test
    void reviewFiles_withReviewableFiles_aggregatesAnalyzerFindings() {
        CodeAnalyzer codeAnalyzer = mock(CodeAnalyzer.class);
        GitHubPullRequestReviewer gitHubPullRequestReviewer =
                new GitHubPullRequestReviewer(codeAnalyzer);

        ReviewablePullRequestFile firstFile = new ReviewablePullRequestFile(
                "src/main/java/PaymentService.java",
                "Java",
                "String status = \"ok\";\nSystem.out.println(\"Processing payment\");",
                List.of(12, 14));

        ReviewablePullRequestFile secondFile = new ReviewablePullRequestFile(
                "src/main/java/OrderService.java",
                "Java",
                "public class OrderService {\nString token = \"secret123\";\n}",
                List.of(30, 31, 32));

        ReviewFinding firstFinding = new ReviewFinding(
                "src/main/java/PaymentService.java",
                2,
                ReviewCategory.MAINTAINABILITY,
                ReviewSeverity.LOW,
                "Avoid System.out.println in application code. Use a logger instead.");

        ReviewFinding secondFinding = new ReviewFinding(
                "src/main/java/OrderService.java",
                2,
                ReviewCategory.SECURITY,
                ReviewSeverity.HIGH,
                "Possible hardcoded secret detected. Store sensitive values in environment variables or a secret manager.");

        when(codeAnalyzer.analyze(new ReviewRequest(
                "src/main/java/PaymentService.java",
                "Java",
                "String status = \"ok\";\nSystem.out.println(\"Processing payment\");"))).thenReturn(List.of(firstFinding));

        when(codeAnalyzer.analyze(new ReviewRequest(
                "src/main/java/OrderService.java",
                "Java",
                "public class OrderService {\nString token = \"secret123\";\n}"))).thenReturn(List.of(secondFinding));

        GitHubPullRequestReviewResult result = gitHubPullRequestReviewer.reviewFiles(List.of(firstFile, secondFile));

        assertThat(result.reviewedFiles()).isEqualTo(2);
        assertThat(result.totalFindings()).isEqualTo(2);
        assertThat(result.findings()).containsExactly(
                new ReviewFinding(
                        "src/main/java/PaymentService.java",
                        14,
                        ReviewCategory.MAINTAINABILITY,
                        ReviewSeverity.LOW,
                        "Avoid System.out.println in application code. Use a logger instead."),
                new ReviewFinding(
                        "src/main/java/OrderService.java",
                        31,
                        ReviewCategory.SECURITY,
                        ReviewSeverity.HIGH,
                        "Possible hardcoded secret detected. Store sensitive values in environment variables or a secret manager."));

        verify(codeAnalyzer).analyze(new ReviewRequest(
                "src/main/java/PaymentService.java",
                "Java",
                "String status = \"ok\";\nSystem.out.println(\"Processing payment\");"));
        verify(codeAnalyzer).analyze(new ReviewRequest(
                "src/main/java/OrderService.java",
                "Java",
                "public class OrderService {\nString token = \"secret123\";\n}"));
    }

    @Test
    void reviewFiles_withNullOrBlankFiles_skipsThem() {
        CodeAnalyzer codeAnalyzer = mock(CodeAnalyzer.class);
        GitHubPullRequestReviewer gitHubPullRequestReviewer = new GitHubPullRequestReviewer(codeAnalyzer);

        GitHubPullRequestReviewResult result = gitHubPullRequestReviewer.reviewFiles(Arrays.asList(
                new ReviewablePullRequestFile(
                        "src/main/java/PaymentService.java",
                        "Java",
                        "   ",
                        List.of(1)),
                null));

        assertThat(result.reviewedFiles()).isZero();
        assertThat(result.totalFindings()).isZero();
        assertThat(result.findings()).isEmpty();

        verifyNoInteractions(codeAnalyzer);
    }

    @Test
    void reviewFiles_withNullInput_returnsEmptyResult() {
        CodeAnalyzer codeAnalyzer = mock(CodeAnalyzer.class);
        GitHubPullRequestReviewer gitHubPullRequestReviewer = new GitHubPullRequestReviewer(codeAnalyzer);

        GitHubPullRequestReviewResult result = gitHubPullRequestReviewer.reviewFiles(null);

        assertThat(result.reviewedFiles()).isZero();
        assertThat(result.totalFindings()).isZero();
        assertThat(result.findings()).isEmpty();

        verifyNoInteractions(codeAnalyzer);
    }
}
