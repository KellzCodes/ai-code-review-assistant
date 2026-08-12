package com.kellidavis.codereviewassistant.review;

public record ReviewFinding(
        String filePath,
        int lineNumber,
        ReviewCategory category,
        ReviewSeverity severity,
        String message
) {
}