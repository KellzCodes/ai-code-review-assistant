package com.kellidavis.codereviewassistant.review.analysis;

import com.kellidavis.codereviewassistant.review.ReviewCategory;
import com.kellidavis.codereviewassistant.review.ReviewFinding;
import com.kellidavis.codereviewassistant.review.ReviewRequest;
import com.kellidavis.codereviewassistant.review.ReviewSeverity;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class RuleBasedCodeAnalyzer implements CodeAnalyzer {
    private static final Pattern HARDCODED_SECRET_PATTERN =
            Pattern.compile(
                    "\\b(password|apiKey|api_key|secret|token|accessToken|access_token)\\b"
                    + "\\s*=\\s*\"[^\"]+\"",
                    Pattern.CASE_INSENSITIVE
            );

    @Override
    public List<ReviewFinding> analyze(ReviewRequest request) {
        List<ReviewFinding> findings = new ArrayList<>();
        List<String> codeLines = request.code().lines().toList();

        for(int i = 0; i < codeLines.size(); i++) {
            String line = codeLines.get(i);
            int lineNumber = i + 1;

            if(line.contains("System.out.println")){
                findings.add(new ReviewFinding(
                        request.filePath(),
                        lineNumber,
                        ReviewCategory.MAINTAINABILITY,
                        ReviewSeverity.LOW,
                        "Avoid System.out.println in application code. Use a logger instead."
                ));
            }

            if(HARDCODED_SECRET_PATTERN.matcher(line).find()){
                findings.add(new ReviewFinding(
                        request.filePath(),
                        lineNumber,
                        ReviewCategory.SECURITY,
                        ReviewSeverity.HIGH,
                        "Possible hardcoded secret detected. Store sensitive values in environment variables or a secret manager."
                ));
            }
        }

        return List.copyOf(findings);
    }
}
