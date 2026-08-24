package com.kellidavis.codereviewassistant.github;

public class InvalidGitHubWebhookPayloadException extends RuntimeException{
    public InvalidGitHubWebhookPayloadException(String message){
        super(message);
    }
}
