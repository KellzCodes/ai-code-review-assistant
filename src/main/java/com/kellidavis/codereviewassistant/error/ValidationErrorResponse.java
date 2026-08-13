package com.kellidavis.codereviewassistant.error;

import java.time.Instant;
import java.util.Map;

public record ValidationErrorResponse(
        Instant timestamp,
        int status,
        String error,
        Map<String, String> fieldErrors
) {
}
