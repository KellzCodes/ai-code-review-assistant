package com.kellidavis.codereviewassistant.github;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github/webhooks")
public class GitHubWebhookController {
    private final GitHubWebhookService gitHubWebhookService;

    public GitHubWebhookController(GitHubWebhookService gitHubWebhookService) {
        this.gitHubWebhookService = gitHubWebhookService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GitHubWebhookResponse receiveWebhook(
            @RequestHeader("X-GitHub-Event")
            String eventType,

            @RequestHeader("X-GitHub-Delivery")
            String deliveryId,

            @Valid @RequestBody
            GitHubPullRequestEvent event
    ){
        return gitHubWebhookService.handle(eventType, deliveryId, event);
    }
}
