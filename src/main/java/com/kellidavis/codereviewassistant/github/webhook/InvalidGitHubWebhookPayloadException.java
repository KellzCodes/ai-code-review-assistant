package com.kellidavis.codereviewassistant.github.webhook;

public class InvalidGitHubWebhookPayloadException extends RuntimeException{
    public InvalidGitHubWebhookPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
