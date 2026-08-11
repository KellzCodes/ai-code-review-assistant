package com.kellidavis.codereviewassistant.review;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController
{
    private final ReviewService reviewService;
    public ReviewController(ReviewService reviewService)
    {
        this.reviewService = reviewService;
    }
    @GetMapping("/test")
    public ReviewFinding testReview()
    {
        return reviewService.getReviewFinding();
    }
}