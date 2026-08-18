package com.kellidavis.codereviewassistant.review.analysis;

import com.kellidavis.codereviewassistant.review.ReviewFinding;
import com.kellidavis.codereviewassistant.review.ReviewRequest;
import java.util.List;

public interface CodeAnalyzer {
    List<ReviewFinding> analyze(ReviewRequest request);
}
