package com.kellidavis.codereviewassistant.github.review;

import java.util.List;

public record ReviewablePullRequestFile(
        String filePath,
        String language,
        String reviewableCode,
        List<Integer> fileLineNumbers
) {
    public ReviewablePullRequestFile {
        fileLineNumbers = List.copyOf(fileLineNumbers);
    }

    public int mapToFileLineNumber(int reviewableLineNumber) {
        if (reviewableLineNumber < 1 || reviewableLineNumber > fileLineNumbers.size()) {
            return reviewableLineNumber;
        }

        return fileLineNumbers.get(reviewableLineNumber - 1);
    }
}
