package com.kellidavis.codereviewassistant.review;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewService {
    public ReviewResponse reviewCode(ReviewRequest request){
        List<ReviewFinding> findings = new ArrayList<>();
        List<String> codeLines = request.code().lines().toList();

        for(int i = 0; i < codeLines.size(); i++){
            String line = codeLines.get(i);

            if(line.contains("System.out.println")){
                ReviewFinding finding = new ReviewFinding(
                        request.filePath(),
                        i + 1,
                        ReviewCategory.MAINTAINABILITY,
                        ReviewSeverity.LOW,
                        "Avoid System.out.println in application code. Use a logger instead."
                );

                findings.add(finding);
            }
        }

        return new ReviewResponse(List.copyOf(findings));
    }
}
