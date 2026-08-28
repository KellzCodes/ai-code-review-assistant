package com.kellidavis.codereviewassistant.github.webhook;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.util.Set;

@Component
public class GitHubWebhookPayloadParser {
    private final JsonMapper jsonMapper;
    private final Validator validator;

    public GitHubWebhookPayloadParser(JsonMapper jsonMapper, Validator validator) {
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    public GitHubPullRequestEvent parse(byte[] payload){
        GitHubPullRequestEvent event;

        try{
            event = jsonMapper.readValue(payload, GitHubPullRequestEvent.class);
        }catch(JacksonException e){
            throw new InvalidGitHubWebhookPayloadException("Webhook payload must contain valid JSON.", e);
        }

        Set<ConstraintViolation<GitHubPullRequestEvent>> violations = validator.validate(event);

        if(!violations.isEmpty()){
            throw new ConstraintViolationException(violations);
        }

        return event;
    }
}
