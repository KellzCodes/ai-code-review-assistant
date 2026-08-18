package com.kellidavis.codereviewassistant.review;

import com.kellidavis.codereviewassistant.review.analysis.CodeAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewServiceTest {

    @Test
    void reviewCode_returnsFindingsProducedByAnalyzer() {
        CodeAnalyzer codeAnalyzer = mock(CodeAnalyzer.class);
        ReviewService reviewService =
                new ReviewService(codeAnalyzer);

        ReviewRequest request = new ReviewRequest(
                "src/main/java/PaymentService.java",
                "Java",
                "System.out.println(\"Processing payment\");"
        );

        ReviewFinding expectedFinding = new ReviewFinding(
                "src/main/java/PaymentService.java",
                1,
                ReviewCategory.MAINTAINABILITY,
                ReviewSeverity.LOW,
                "Avoid System.out.println in application code. Use a logger instead."
        );

        when(codeAnalyzer.analyze(request))
                .thenReturn(List.of(expectedFinding));

        ReviewResponse response =
                reviewService.reviewCode(request);

        assertThat(response.findings())
                .containsExactly(expectedFinding);

        verify(codeAnalyzer).analyze(request);
    }
}