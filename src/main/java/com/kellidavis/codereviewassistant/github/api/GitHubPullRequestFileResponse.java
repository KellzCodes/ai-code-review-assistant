package com.kellidavis.codereviewassistant.github.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullRequestFileResponse(
        String filename,
        String status,
        String patch,
        int additions,
        int deletions,
        int changes,
        @JsonProperty("previous_filename")
        String previousFilename
) {
}