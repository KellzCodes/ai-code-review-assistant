package com.kellidavis.codereviewassistant.github;

import com.kellidavis.codereviewassistant.review.ReviewFinding;
import java.util.List;

public record GitHubPullRequestReviewResult(List<ReviewFinding> findings, int reviewedFiles) {
    public GitHubPullRequestReviewResult {
        findings = List.copyOf(findings);
    }

    public int totalFindings() {
        return findings.size();
    }
}
