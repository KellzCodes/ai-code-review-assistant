package com.kellidavis.codereviewassistant.github;

public class InvalidGitHubWebhookPayloadException extends RuntimeException{
    public InvalidGitHubWebhookPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
