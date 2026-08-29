package com.kellidavis.codereviewassistant.github.review;

import com.kellidavis.codereviewassistant.github.webhook.GitHubPullRequestEvent;
import com.kellidavis.codereviewassistant.review.ReviewFinding;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;

@Component
public class GitHubPullRequestReviewCommentFormatter {
    public String format(GitHubPullRequestEvent event, PullRequestFilePreparationResult preparationResult,
            GitHubPullRequestReviewResult reviewResult) {

        StringBuilder comment = new StringBuilder();

        comment.append("## AI Code Review Summary\n\n");
        comment.append("Pull request: [#").append(event.number()).append(" ").append(event.pullRequest().title())
                .append("](").append(event.pullRequest().htmlUrl()).append(")\n");
        comment.append("Repository: `").append(event.repository().fullName()).append("`\n\n");
        comment.append("Changed files: ").append(preparationResult.totalChangedFiles()).append("\n");
        comment.append("Prepared files: ").append(preparationResult.preparedFiles().size()).append("\n");
        comment.append("Skipped files: ").append(preparationResult.skippedFiles()).append("\n");
        comment.append("Reviewed files: ").append(reviewResult.reviewedFiles()).append("\n");
        comment.append("Total findings: ").append(reviewResult.totalFindings()).append("\n\n");

        if (reviewResult.reviewedFiles() == 0) {
            comment.append("### Result\n\n");
            comment.append("No reviewable added lines were available to analyze in this pull request.\n\n");
            comment.append("_Generated automatically by the AI Code Review Assistant._");
            return comment.toString();
        }

        if (reviewResult.findings().isEmpty()) {
            comment.append("### Result\n\n");
            comment.append("No review findings were generated from the analyzed pull request changes.\n\n");
            comment.append("_Generated automatically by the AI Code Review Assistant._");
            return comment.toString();
        }

        comment.append("### Findings\n\n");

        List<ReviewFinding> sortedFindings = reviewResult.findings().stream().sorted(Comparator
                .comparing(this::severityRank).thenComparing(ReviewFinding::filePath)
                .thenComparingInt(ReviewFinding::lineNumber)
                .thenComparing(finding -> finding.category().name())).toList();

        int findingIndex = 1;
        for (ReviewFinding finding : sortedFindings) {
            comment.append(findingIndex).append(". **[").append(finding.severity()).append("] ").append(finding.category())
                    .append("** `").append(finding.filePath()).append(":").append(finding.lineNumber()).append("`\n");
            comment.append("   ").append(finding.message()).append("\n");
            findingIndex++;
        }

        comment.append("\n_Generated automatically by the AI Code Review Assistant._");
        return comment.toString();
    }

    private int severityRank(ReviewFinding finding) {
        return switch (finding.severity()) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }
}
