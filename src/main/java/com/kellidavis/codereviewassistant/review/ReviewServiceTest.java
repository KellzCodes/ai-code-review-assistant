package com.kellidavis.codereviewassistant.review;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewServiceTest {
    @Test
    void reviewCode_usesSubmittedFilePath() {
        ReviewService reviewService = new ReviewService();

        ReviewRequest request = new ReviewRequest(
                "src/main/java/OrderService.java",
                "Java",
                "public void processOrder() {}"
        );

        ReviewResponse response = reviewService.reviewCode(request);

        assertThat(response.findings()).hasSize(1);

        ReviewFinding finding = response.findings().get(0);

        assertThat(finding.filePath()).isEqualTo(request.filePath());
        assertThat(finding.lineNumber()).isEqualTo(12);
        assertThat(finding.category()).isEqualTo(ReviewCategory.BUG);
        assertThat(finding.severity()).isEqualTo(ReviewSeverity.HIGH);
        assertThat(finding.message()).isEqualTo("This value could be null.");
    }
}
