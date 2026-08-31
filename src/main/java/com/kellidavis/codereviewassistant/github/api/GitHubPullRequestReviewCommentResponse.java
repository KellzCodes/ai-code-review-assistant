package com.kellidavis.codereviewassistant.github.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullRequestReviewCommentResponse(
        Long id,
        String body,
        @JsonProperty("html_url")
        String htmlUrl,
        String path,
        Integer line
) {
}
