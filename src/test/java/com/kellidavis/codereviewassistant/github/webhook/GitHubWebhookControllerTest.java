package com.kellidavis.codereviewassistant.github.webhook;

import com.kellidavis.codereviewassistant.error.GlobalExceptionHandler;
import com.kellidavis.codereviewassistant.github.api.GitHubApiException;
import com.kellidavis.codereviewassistant.review.ReviewCategory;
import com.kellidavis.codereviewassistant.review.ReviewFinding;
import com.kellidavis.codereviewassistant.review.ReviewSeverity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GitHubWebhookController.class)
@Import(GlobalExceptionHandler.class)
class GitHubWebhookControllerTest {
    private static final String PAYLOAD = """
            {
              "action": "opened",
              "number": 42,
              "pull_request": {
                "title": "Add payment validation",
                "html_url": "https://github.com/kellidavis/ai-code-review-assistant/pull/42"
              },
              "repository": {
                "full_name": "kellidavis/ai-code-review-assistant"
              }
            }
            """;

    /*
     * This payload intentionally has no comma after
     * "Add payment validation".
     */
    private static final String MALFORMED_PAYLOAD = """
            {
              "action": "opened",
              "number": 42,
              "pull_request": {
                "title": "Add payment validation"
                "html_url": "https://github.com/kellidavis/ai-code-review-assistant/pull/42"
              },
              "repository": {
                "full_name": "kellidavis/ai-code-review-assistant"
              }
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitHubWebhookSignatureVerifier signatureVerifier;

    @MockitoBean
    private GitHubWebhookPayloadParser payloadParser;

    @MockitoBean
    private GitHubWebhookService gitHubWebhookService;

    @Test
    void receiveWebhook_withValidSignature_returnsAccepted() throws Exception {
        GitHubPullRequestEvent event = createEvent();

        GitHubWebhookResponse response = new GitHubWebhookResponse(
                "ACCEPTED",
                "delivery-123",
                "pull_request",
                "opened",
                "kellidavis/ai-code-review-assistant",
                42,
                3,
                2,
                1,
                2,
                2,
                true,
                "https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1",
                List.of(
                        new ReviewFinding(
                                "src/main/java/PaymentService.java",
                                2,
                                ReviewCategory.MAINTAINABILITY,
                                ReviewSeverity.LOW,
                                "Avoid System.out.println in application code. Use a logger instead."),
                        new ReviewFinding(
                                "src/main/java/OrderService.java",
                                2,
                                ReviewCategory.SECURITY,
                                ReviewSeverity.HIGH,
                                "Possible hardcoded secret detected. Store sensitive values in environment variables or a secret manager.")),
                "Pull request event accepted and 2 file(s) were prepared from 3 changed file(s). 1 file(s) were skipped. 2 file(s) were analyzed and 2 review finding(s) were generated. A summary comment was posted to the pull request.");

        when(signatureVerifier.isValid(any(byte[].class), eq("sha256=valid"))).thenReturn(true);

        when(payloadParser.parse(any(byte[].class))).thenReturn(event);

        when(gitHubWebhookService.handle("pull_request", "delivery-123", event)).thenReturn(response);

        mockMvc.perform(post("/api/github/webhooks")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "delivery-123")
                        .header("X-Hub-Signature-256", "sha256=valid")
                        .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.deliveryId").value("delivery-123"))
                .andExpect(jsonPath("$.eventType").value("pull_request"))
                .andExpect(jsonPath("$.action").value("opened"))
                .andExpect(jsonPath("$.repository").value("kellidavis/ai-code-review-assistant"))
                .andExpect(jsonPath("$.pullRequestNumber").value(42))
                .andExpect(jsonPath("$.totalChangedFiles").value(3))
                .andExpect(jsonPath("$.preparedFiles").value(2))
                .andExpect(jsonPath("$.skippedFiles").value(1))
                .andExpect(jsonPath("$.reviewedFiles").value(2))
                .andExpect(jsonPath("$.totalFindings").value(2))
                .andExpect(jsonPath("$.summaryCommentPosted").value(true))
                .andExpect(jsonPath("$.summaryCommentUrl")
                        .value("https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1"))
                .andExpect(jsonPath("$.findings[0].filePath").value("src/main/java/PaymentService.java"))
                .andExpect(jsonPath("$.findings[0].lineNumber").value(2))
                .andExpect(jsonPath("$.findings[1].filePath").value("src/main/java/OrderService.java"))
                .andExpect(jsonPath("$.findings[1].lineNumber").value(2))
                .andExpect(jsonPath("$.message")
                        .value("Pull request event accepted and 2 file(s) were prepared from 3 changed file(s). 1 file(s) were skipped. 2 file(s) were analyzed and 2 review finding(s) were generated. A summary comment was posted to the pull request."));

        verify(payloadParser).parse(any(byte[].class));
        verify(gitHubWebhookService).handle("pull_request", "delivery-123", event);
    }

    @Test
    void receiveWebhook_withInvalidSignature_returnsUnauthorized() throws Exception {
        when(signatureVerifier.isValid(any(byte[].class), eq("sha256=wrong"))).thenReturn(false);

        mockMvc.perform(post("/api/github/webhooks")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "delivery-123")
                        .header("X-Hub-Signature-256", "sha256=wrong")
                        .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid or missing GitHub webhook signature."));

        verifyNoInteractions(payloadParser, gitHubWebhookService);
    }

    @Test
    void receiveWebhook_withoutSignature_returnsUnauthorized() throws Exception {
        when(signatureVerifier.isValid(any(byte[].class), isNull())).thenReturn(false);

        mockMvc.perform(post("/api/github/webhooks")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "delivery-123")
                        .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(payloadParser, gitHubWebhookService);
    }

    @Test
    void receiveWebhook_withoutEventHeader_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/github/webhooks")
                        .header("X-GitHub-Delivery", "delivery-123")
                        .header("X-Hub-Signature-256", "sha256=valid")
                        .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD)).andExpect(status().isBadRequest());

        verifyNoInteractions(payloadParser, gitHubWebhookService);
    }

    @Test
    void receiveWebhook_withValidSignatureAndMalformedJson_returnsBadRequest() throws Exception {
        when(signatureVerifier.isValid(any(byte[].class), eq("sha256=valid"))).thenReturn(true);

        when(payloadParser.parse(any(byte[].class))).thenThrow(new InvalidGitHubWebhookPayloadException(
                        "Webhook payload must contain valid JSON.", new RuntimeException("Test JSON parsing failure")));

        mockMvc.perform(post("/api/github/webhooks")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "delivery-123")
                        .header("X-Hub-Signature-256", "sha256=valid")
                        .contentType(MediaType.APPLICATION_JSON).content(MALFORMED_PAYLOAD))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Webhook payload must contain valid JSON."));

        verify(payloadParser).parse(any(byte[].class));
        verifyNoInteractions(gitHubWebhookService);
    }

    @Test
    void receiveWebhook_whenGitHubApiFails_returnsBadGateway() throws Exception {
        GitHubPullRequestEvent event = createEvent();

        when(signatureVerifier.isValid(any(byte[].class), eq("sha256=valid"))).thenReturn(true);

        when(payloadParser.parse(any(byte[].class))).thenReturn(event);

        when(gitHubWebhookService.handle("pull_request", "delivery-123", event))
                .thenThrow(new GitHubApiException(
                        "Failed to fetch pull request files for kellidavis/ai-code-review-assistant#42."));

        mockMvc.perform(post("/api/github/webhooks")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "delivery-123")
                        .header("X-Hub-Signature-256", "sha256=valid")
                        .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD)).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message")
                        .value("Failed to fetch pull request files for kellidavis/ai-code-review-assistant#42."));

        verify(payloadParser).parse(any(byte[].class));
        verify(gitHubWebhookService).handle("pull_request", "delivery-123", event);
    }

    private GitHubPullRequestEvent createEvent() {
        return new GitHubPullRequestEvent(
                "opened",
                42,
                new GitHubPullRequest(
                        "Add payment validation",
                        "https://github.com/kellidavis/ai-code-review-assistant/pull/42"),
                new GitHubRepository("kellidavis/ai-code-review-assistant"));
    }
}
