package com.kellidavis.codereviewassistant.github.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullRequestEvent(
        @NotBlank(message = "Webhook action is required")
        String action,

        @Positive(message = "Pull request number must be positive")
        int number,

        @Valid
        @NotNull(message = "Pull request information is required")
        @JsonProperty("pull_request")
        GitHubPullRequest pullRequest,

        @Valid
        @NotNull(message = "Repository information is required")
        GitHubRepository repository
) {
}
