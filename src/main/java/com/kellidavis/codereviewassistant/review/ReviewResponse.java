package com.kellidavis.codereviewassistant.review;

import java.util.List;

public record ReviewResponse(
        List<ReviewFinding> findings
) {
}
