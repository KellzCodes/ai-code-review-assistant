package com.kellidavis.codereviewassistant.github.review;

import com.kellidavis.codereviewassistant.review.ReviewFinding;
import com.kellidavis.codereviewassistant.review.ReviewRequest;
import com.kellidavis.codereviewassistant.review.analysis.CodeAnalyzer;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class GitHubPullRequestReviewer {
    private final CodeAnalyzer codeAnalyzer;

    public GitHubPullRequestReviewer(CodeAnalyzer codeAnalyzer) {
        this.codeAnalyzer = codeAnalyzer;
    }

    public GitHubPullRequestReviewResult reviewFiles(List<ReviewablePullRequestFile> reviewableFiles) {
        if (reviewableFiles == null) {
            return new GitHubPullRequestReviewResult(List.of(), 0);
        }

        List<ReviewFinding> findings = new ArrayList<>();
        int reviewedFiles = 0;

        for (ReviewablePullRequestFile reviewableFile : reviewableFiles) {
            if (shouldSkip(reviewableFile)) {
                continue;
            }

            for (ReviewFinding finding : codeAnalyzer.analyze(new ReviewRequest(
                    reviewableFile.filePath(),
                    reviewableFile.language(),
                    reviewableFile.reviewableCode()))) {
                findings.add(mapFindingToFileLine(reviewableFile, finding));
            }
            reviewedFiles++;
        }

        return new GitHubPullRequestReviewResult(findings, reviewedFiles);
    }

    private ReviewFinding mapFindingToFileLine(ReviewablePullRequestFile reviewableFile, ReviewFinding finding) {
        return new ReviewFinding(
                reviewableFile.filePath(),
                reviewableFile.mapToFileLineNumber(finding.lineNumber()),
                finding.category(),
                finding.severity(),
                finding.message());
    }

    private boolean shouldSkip(ReviewablePullRequestFile reviewableFile) {
        return reviewableFile == null
                || reviewableFile.filePath() == null
                || reviewableFile.filePath().isBlank()
                || reviewableFile.language() == null
                || reviewableFile.language().isBlank()
                || reviewableFile.reviewableCode() == null
                || reviewableFile.reviewableCode().isBlank()
                || reviewableFile.fileLineNumbers() == null
                || reviewableFile.fileLineNumbers().isEmpty();
    }
}
