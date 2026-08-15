package com.kellidavis.codereviewassistant.review;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {
    public ReviewResponse reviewCode(ReviewRequest request){
        ReviewFinding finding = new ReviewFinding(
                request.filePath(),
                12,
                ReviewCategory.BUG,
                ReviewSeverity.HIGH,
                "This value could be null."
        );

        return new ReviewResponse(List.of(finding));
    }
}
