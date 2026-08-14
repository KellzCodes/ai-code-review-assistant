package com.kellidavis.codereviewassistant.review;

import com.kellidavis.codereviewassistant.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@Import(GlobalExceptionHandler.class)
public class ReviewControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @Test
    void reviewCode_withValidRequest_returnsFinding() throws Exception {
        ReviewFinding finding = new ReviewFinding(
                "src/main/java/ReviewService.java",
                12,
                ReviewCategory.BUG,
                ReviewSeverity.HIGH,
                "This value could be null."
        );

        when(reviewService.reviewCode(any(ReviewRequest.class))).thenReturn(finding);

        String requestJson = """
                {
                 "filePath": "src/main/java/ReviewService.java",
                 "language": "Java",
                 "code": "public void processReview() {}"   
                }
                """;

        mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.filePath")
                        .value("src/main/java/ReviewService.java"))
                .andExpect(jsonPath("$.lineNumber").value(12))
                .andExpect(jsonPath("$.category").value("BUG"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.message").value("This value could be null."));

        verify(reviewService).reviewCode(any(ReviewRequest.class));
    }

    @Test
    void reviewCode_withBlankCode_returnsValidationError() throws Exception {
        String requestJson = """
                {
                    "filePath": "src/main/java/ReviewService.java",
                    "language": "Java",
                    "code": ""
                }
                """;

        mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.code")
                        .value("Code cannot be blank"));

        verify(reviewService, never())
                .reviewCode(any(ReviewRequest.class));
    }

    @Test
    void reviewCode_withAllFieldsBlank_returnsAllFieldErrors() throws Exception {
        String requestJson = """
                 {
                    "filePath": "",
                    "language": "",
                    "code": ""
                 }
                """;

        mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("fieldErrors.filePath").exists())
                .andExpect(jsonPath("$.fieldErrors.language").exists())
                .andExpect(jsonPath("$.fieldErrors.code").exists());

        verify(reviewService, never())
                .reviewCode(any(ReviewRequest.class));
    }

    @Test
    void reviewCode_withLanguageTooLong_returnsValidationError() throws Exception {
        String longLanguage = "J".repeat(51);

        String requestJson = """
        {
          "filePath": "src/main/java/PaymentService.java",
          "language": "%s",
          "code": "public void processPayment() {}"
        }
        """.formatted(longLanguage);

        mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.language").exists());

        verify(reviewService, never())
                .reviewCode(any(ReviewRequest.class));
    }
}
