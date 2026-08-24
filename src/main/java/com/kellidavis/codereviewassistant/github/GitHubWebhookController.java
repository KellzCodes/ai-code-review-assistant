package com.kellidavis.codereviewassistant.github;

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

    private final GitHubWebhookSignatureVerifier signatureVerifier;

    private final GitHubWebhookPayloadParser payloadParser;

    private final GitHubWebhookService gitHubWebhookService;

    public GitHubWebhookController(
            GitHubWebhookSignatureVerifier signatureVerifier,
            GitHubWebhookPayloadParser payloadParser,
            GitHubWebhookService gitHubWebhookService
    ) {
        this.signatureVerifier = signatureVerifier;
        this.payloadParser = payloadParser;
        this.gitHubWebhookService = gitHubWebhookService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GitHubWebhookResponse receiveWebhook(
            @RequestHeader("X-GitHub-Event")
            String eventType,

            @RequestHeader("X-GitHub-Delivery")
            String deliveryId,

            @RequestHeader(
                    value = "X-Hub-Signature-256",
                    required = false
            )
            String providedSignature,

            @RequestBody
            byte[] rawPayload
    ) {
        if (!signatureVerifier.isValid(
                rawPayload,
                providedSignature
        )) {
            throw new InvalidGitHubWebhookSignatureException();
        }

        GitHubPullRequestEvent event =
                payloadParser.parse(rawPayload);

        return gitHubWebhookService.handle(
                eventType,
                deliveryId,
                event
        );
    }
}