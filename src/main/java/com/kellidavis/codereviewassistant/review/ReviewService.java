package com.kellidavis.codereviewassistant.review;

import com.kellidavis.codereviewassistant.review.analysis.CodeAnalyzer;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {
    private final CodeAnalyzer codeAnalyzer;

    public ReviewService(CodeAnalyzer codeAnalyzer) {
        this.codeAnalyzer = codeAnalyzer;
    }

    public ReviewResponse reviewCode(ReviewRequest request){
        return new ReviewResponse(codeAnalyzer.analyze(request));
    }
}
