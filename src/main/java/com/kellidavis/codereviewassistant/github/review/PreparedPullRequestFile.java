package com.kellidavis.codereviewassistant.github.review;

public record PreparedPullRequestFile(
        String filePath,
        String language,
        String changeStatus,
        String patch,
        int additions,
        int deletions
) {
}
