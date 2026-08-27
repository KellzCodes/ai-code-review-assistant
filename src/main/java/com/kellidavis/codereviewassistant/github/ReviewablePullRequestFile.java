package com.kellidavis.codereviewassistant.github;

public record ReviewablePullRequestFile(
        String filePath,
        String language,
        String reviewableCode
) {
}
