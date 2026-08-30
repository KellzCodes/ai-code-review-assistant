package com.kellidavis.codereviewassistant.github.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullRequestCommentResponse(
        Long id,
        String body,
        @JsonProperty("html_url")
        String htmlUrl
) {
    public GitHubPullRequestCommentResponse(String htmlUrl) {
        this(null, null, htmlUrl);
    }
}
