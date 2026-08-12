package com.kellidavis.codereviewassistant.review;

public record ReviewRequest(
        String filePath,
        String language,
        String code
) {
}
