package com.kellidavis.codereviewassistant.review;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    @GetMapping("/test")
    public ReviewFinding testReview(){
        return new ReviewFinding(
                "src/main/java/Review.java",
                12,
                ReviewCategory.BUG,
                ReviewSeverity.HIGH,
                "This value could be null."

        );
    }


}
