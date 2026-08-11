package com.kellidavis.codereviewassistant.review;

public record ReviewFinding(
        String filepath,
        int lineNumber,
        ReviewCategory category,
        ReviewSeverity severity,
        String message
) {
}