package com.kellidavis.codereviewassistant.github;

import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class GitHubWebhookService {
    private static final Set<String> REVIEW_ACTIONS = Set.of(
            "opened",
            "reopened",
            "synchronize"
    );

    public GitHubWebhookResponse handle(String eventType, String deliveryId, GitHubPullRequestEvent event){
        if(!"pull_request".equals(eventType)){
            return new GitHubWebhookResponse(
                    "IGNORED",
                    deliveryId,
                    eventType,
                    event.action(),
                    event.repository().fullName(),
                    event.number(),
                    "Webhook event type is not supported."
            );
        }

        if(!REVIEW_ACTIONS.contains(event.action())){
            return new GitHubWebhookResponse(
                    "IGNORED",
                    deliveryId,
                    eventType,
                    event.action(),
                    event.repository().fullName(),
                    event.number(),
                    "Pull request action does not require a code review."
            );
        }

        return new GitHubWebhookResponse(
                "ACCEPTED",
                deliveryId,
                eventType,
                event.action(),
                event.repository().fullName(),
                event.number(),
                "Pull request event accepted for future review processing."
        );
    }
}
