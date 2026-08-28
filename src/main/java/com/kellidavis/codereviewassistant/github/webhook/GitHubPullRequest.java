package com.kellidavis.codereviewassistant.github.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullRequest(
        @NotBlank(message = "Pull request title is required")
        String title,

        @NotBlank(message = "Pull request url is required")
        @JsonProperty("html_url")
        String htmlUrl
) {

}
