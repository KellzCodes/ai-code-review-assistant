package com.kellidavis.codereviewassistant.github.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullRequestHead(
        @NotBlank(message = "Pull request head commit SHA is required")
        String sha
) {
}
