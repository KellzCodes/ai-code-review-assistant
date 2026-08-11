package com.kellidavis.codereviewassistant.review;

import org.springframework.stereotype.Service;

@Service
public class ReviewService {
private final ReviewFinding reviewFinding;

    public ReviewService(){
        this.reviewFinding = new ReviewFinding(
                "src/main/java/Review.java",
                12,
                ReviewCategory.BUG,
                ReviewSeverity.HIGH,
                "This value could be null."
        );
    }

    public ReviewFinding getReviewFinding() {
        return reviewFinding;
    }
}
