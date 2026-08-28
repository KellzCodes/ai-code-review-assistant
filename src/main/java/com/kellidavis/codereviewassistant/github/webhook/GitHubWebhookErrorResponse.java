package com.kellidavis.codereviewassistant.github.webhook;

import java.time.Instant;

public record GitHubWebhookErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message
) {
}