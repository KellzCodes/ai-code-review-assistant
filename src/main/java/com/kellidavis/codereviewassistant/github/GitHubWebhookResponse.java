package com.kellidavis.codereviewassistant.github;

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
        String message
) {
}
