package com.kellidavis.codereviewassistant.github;

import java.time.Instant;

public record GitHubWebhookErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message
) {
}