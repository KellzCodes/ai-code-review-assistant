package com.kellidavis.codereviewassistant.review.analysis;

import com.kellidavis.codereviewassistant.review.ReviewCategory;
import com.kellidavis.codereviewassistant.review.ReviewFinding;
import com.kellidavis.codereviewassistant.review.ReviewRequest;
import com.kellidavis.codereviewassistant.review.ReviewSeverity;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class RuleBasedCodeAnalyzer implements CodeAnalyzer {

    @Override
    public List<ReviewFinding> analyze(ReviewRequest request){
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

        return List.copyOf(findings);
    }
}
