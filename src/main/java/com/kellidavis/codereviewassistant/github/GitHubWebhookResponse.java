package com.kellidavis.codereviewassistant.github;

import com.kellidavis.codereviewassistant.review.ReviewFinding;
import java.util.List;

public record GitHubWebhookResponse(
        String status,
        String deliveryId,
        String eventType,
        String action,
        String repository,
        int pullRequestNumber,
        int totalChangedFiles,
        int preparedFiles,
        int skippedFiles,
        int reviewedFiles,
        int totalFindings,
        List<ReviewFinding> findings,
        String message
) {
    public GitHubWebhookResponse {
        findings = List.copyOf(findings);
    }
}
