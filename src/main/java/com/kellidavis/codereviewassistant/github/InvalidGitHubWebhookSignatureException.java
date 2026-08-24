package com.kellidavis.codereviewassistant.github;

public class InvalidGitHubWebhookSignatureException extends RuntimeException {
    public InvalidGitHubWebhookSignatureException() {
        super("Invalid or missing GitHub webhook signature.");
    }
}
