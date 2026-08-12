package com.kellidavis.codereviewassistant.review;

import org.springframework.stereotype.Service;

@Service
public class ReviewService {
    public ReviewFinding reviewCode(ReviewRequest request){
        return new ReviewFinding(
                request.filePath(),
                12,
                ReviewCategory.BUG,
                ReviewSeverity.HIGH,
                "This value could be null."
        );
    }
}
