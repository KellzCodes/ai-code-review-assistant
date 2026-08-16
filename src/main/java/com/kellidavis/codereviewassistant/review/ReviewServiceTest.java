package com.kellidavis.codereviewassistant.review;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewServiceTest {
    private final ReviewService reviewService = new ReviewService();

    @Test
    void reviewCode_withSystemOutPrintln_returnsFinding(){
        ReviewRequest request = new ReviewRequest(
                "src/main/java/PaymentService.java",
                "Java",
                """
                        public class PaymentService {
                            public void processPayment() {
                                System.out.println("Processing payment");
                            }
                        }
                      """
        );

        ReviewResponse response = reviewService.reviewCode(request);
        assertThat(response.findings()).hasSize(1);

        ReviewFinding finding = response.findings().get(0);
        assertThat(finding.filePath())
                .isEqualTo("src/main/java/PaymentService.java");
        assertThat(finding.lineNumber()).isEqualTo(3);
        assertThat(finding.category()).isEqualTo(ReviewCategory.MAINTAINABILITY);
        assertThat(finding.severity()).isEqualTo(ReviewSeverity.LOW);
        assertThat(finding.message())
                .isEqualTo("Avoid System.out.println in application code. Use a logger instead.");

    }

    @Test
    void reviewCode_withoutSystemOutPrintln_returnsEmptyFindings(){
        ReviewRequest request = new ReviewRequest(
                "src/main/java/PaymentService.java",
                "Java",
                """
                       public class PaymentService {
                            public void processPayment() {
                                process();
                            }
                        }
                      """
        );

        ReviewResponse response = reviewService.reviewCode(request);
        assertThat(response.findings()).isEmpty();
    }

    @Test
    void reviewCode_withMultipleSystemOutPrintlnStatements_returnsMultipleFindings(){
        ReviewRequest request = new ReviewRequest(
                "src/main/java/PaymentService.java",
                "Java",
                """
                      public class PaymentService {
                            System.out.println("First");
                            System.out.println("Second");
                      """
        );

        ReviewResponse response = reviewService.reviewCode(request);
        assertThat(response.findings()).hasSize(2);
        assertThat(response.findings().get(0).lineNumber()).isEqualTo(2);
        assertThat(response.findings().get(1).lineNumber()).isEqualTo(3);
    }
}
