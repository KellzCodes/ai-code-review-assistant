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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GitHubWebhookController.class)
@Import(GlobalExceptionHandler.class)
class GitHubWebhookControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitHubWebhookService gitHubWebhookService;

    @Test
    void receiveWebhook_withValidPullRequest_returnsAcceptedResponse() throws Exception{
        GitHubWebhookResponse response = new GitHubWebhookResponse(
                "ACCEPTED",
                "delivery-123",
                "pull_request",
                "opened",
                "kellidavis/ai-code-review-assistant",
                42,
                "Pull request event accepted for future review processing."
        );

        when(gitHubWebhookService.handle(eq("pull_request"),
                eq("delivery-123"),
                any(GitHubPullRequestEvent.class)
        )).thenReturn(response);

        mockMvc.perform(post("/api/github/webhooks")
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "delivery-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
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
                        """

                ))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.deliveryId").value("delivery-123"))
                .andExpect(jsonPath("$.eventType").value("pull_request"))
                .andExpect(jsonPath("$.action").value("opened"))
                .andExpect(jsonPath("$.repository").value("kellidavis/ai-code-review-assistant"))
                .andExpect(jsonPath("$.pullRequestNumber").value(42));

        verify(gitHubWebhookService).handle(
                eq("pull_request"),
                eq("delivery-123"),
                argThat(event ->
                        event.action().equals("opened")
                                && event.number() == 42
                                && event.pullRequest()
                                .title().equals("Add payment validation")
                        && event.repository().fullName().equals("kellidavis/ai-code-review-assistant")
                )
        );
    }

    @Test
    void receiveWebhook_withBlankAction_returnsValidationError() throws Exception{
        mockMvc.perform(post("/api/github/webhooks")
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "delivery-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "action": "",
                            "number": 42,
                            "pull_request": {
                                "title": "Add payment validation",
                                "html_url": "https://github.com/kellidavis/ai-code-review-assistant/pull/42"
                            },
                            "repository": {
                                "full_name": "kellidavis/ai-code-review-assistant"
                            }
                        }
                        """
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.action").value("Webhook action is required"));

        verifyNoInteractions(gitHubWebhookService);
    }

    @Test
    void receiveWebhook_withoutEventHeader_returnsBadRequest()
            throws Exception {

        mockMvc.perform(post("/api/github/webhooks")
                        .header(
                                "X-GitHub-Delivery",
                                "delivery-123"
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(gitHubWebhookService);
    }
}
