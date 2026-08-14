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

        ReviewFinding finding = reviewService.reviewCode(request);

        assertThat(finding.filePath())
                .isEqualTo("src/main/java/OrderService.java");
    }
}
