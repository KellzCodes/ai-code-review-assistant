package com.kellidavis.codereviewassistant.github;

import com.kellidavis.codereviewassistant.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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

        GitHubWebhookResponse response =
                new GitHubWebhookResponse(
                        "ACCEPTED",
                        "delivery-123",
                        "pull_request",
                        "opened",
                        "kellidavis/ai-code-review-assistant",
                        42,
                        "Pull request event accepted for future review processing."
                );

        when(signatureVerifier.isValid(any(byte[].class), eq("sha256=valid"))).thenReturn(true);

        when(payloadParser.parse(any(byte[].class))).thenReturn(event);

        when(gitHubWebhookService.handle("pull_request", "delivery-123", event))
                .thenReturn(response);

        mockMvc.perform(post("/api/github/webhooks")
                                .header(
                                        "X-GitHub-Event",
                                        "pull_request"
                                )
                                .header(
                                        "X-GitHub-Delivery",
                                        "delivery-123"
                                )
                                .header(
                                        "X-Hub-Signature-256",
                                        "sha256=valid"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(PAYLOAD)).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.deliveryId").value("delivery-123"))
                .andExpect(jsonPath("$.pullRequestNumber").value(42));

        verify(payloadParser).parse(any(byte[].class));

        verify(gitHubWebhookService).handle("pull_request", "delivery-123", event);
    }

    @Test
    void receiveWebhook_withInvalidSignature_returnsUnauthorized() throws Exception {
        when(signatureVerifier.isValid(any(byte[].class), eq("sha256=wrong"))).thenReturn(false);

        mockMvc.perform(post("/api/github/webhooks")
                                .header(
                                        "X-GitHub-Event",
                                        "pull_request")
                                .header(
                                        "X-GitHub-Delivery",
                                        "delivery-123")
                                .header(
                                        "X-Hub-Signature-256",
                                        "sha256=wrong")
                                .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid or missing GitHub webhook signature."));

        verifyNoInteractions(payloadParser, gitHubWebhookService);
    }

    @Test
    void receiveWebhook_withoutSignature_returnsUnauthorized() throws Exception {
        when(signatureVerifier.isValid(any(byte[].class), isNull())).thenReturn(false);

        mockMvc.perform(post("/api/github/webhooks").header("X-GitHub-Event", "pull_request")
                                .header("X-GitHub-Delivery", "delivery-123")
                                .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(payloadParser, gitHubWebhookService);
    }

    @Test
    void receiveWebhook_withoutEventHeader_returnsBadRequest() throws Exception {

        mockMvc.perform(post("/api/github/webhooks").header("X-GitHub-Delivery", "delivery-123")
                                .header("X-Hub-Signature-256", "sha256=valid")
                                .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(payloadParser, gitHubWebhookService);
    }

    @Test
    void receiveWebhook_withValidSignatureAndMalformedJson_returnsBadRequest() throws Exception {
        when(signatureVerifier.isValid(any(byte[].class), eq("sha256=valid"))).thenReturn(true);

        when(payloadParser.parse(any(byte[].class))).thenThrow(new InvalidGitHubWebhookPayloadException
                ("Webhook payload must contain valid JSON.", new RuntimeException("Test JSON parsing failure")));

        mockMvc.perform(post("/api/github/webhooks").header("X-GitHub-Event", "pull_request")
                                .header("X-GitHub-Delivery", "delivery-123")
                                .header("X-Hub-Signature-256", "sha256=valid")
                                .contentType(MediaType.APPLICATION_JSON).content(MALFORMED_PAYLOAD))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Webhook payload must contain valid JSON."));

        verify(payloadParser).parse(any(byte[].class));

        verifyNoInteractions(gitHubWebhookService);
    }

    private GitHubPullRequestEvent createEvent() {
        return new GitHubPullRequestEvent(
                "opened",
                42,
                new GitHubPullRequest("Add payment validation",
                        "https://github.com/kellidavis/ai-code-review-assistant/pull/42"),
                new GitHubRepository("kellidavis/ai-code-review-assistant")
        );
    }
}