package com.kellidavis.codereviewassistant.github.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullRequest(
        @NotBlank(message = "Pull request title is required")
        String title,

        @NotBlank(message = "Pull request url is required")
        @JsonProperty("html_url")
        String htmlUrl,

        @Valid
        @NotNull(message = "Pull request head information is required")
        GitHubPullRequestHead head
) {

}
