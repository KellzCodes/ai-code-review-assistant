package com.kellidavis.codereviewassistant.github.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubRestPullRequestCommentsClientTest {
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void postPullRequestComment_withValidRequest_returnsCommentResponse() {
        GitHubRestPullRequestCommentsClient client =
                new GitHubRestPullRequestCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        mockServer.expect(requestTo("https://api.github.com/repos/kellidavis/ai-code-review-assistant/issues/42/comments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(content().json("""
                        {
                          "body": "## AI Code Review Summary"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "html_url": "https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1"
                        }
                        """, MediaType.APPLICATION_JSON));

        GitHubPullRequestCommentResponse response = client.postPullRequestComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "## AI Code Review Summary");

        assertThat(response.htmlUrl())
                .isEqualTo("https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1");

        mockServer.verify();
    }

    @Test
    void postPullRequestComment_withBlankCommentBody_throwsException() {
        GitHubRestPullRequestCommentsClient client =
                new GitHubRestPullRequestCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        assertThatThrownBy(() -> client.postPullRequestComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "   "
        ))
                .isInstanceOf(GitHubApiException.class)
                .hasMessage("Pull request comment body must not be blank.");
    }

    @Test
    void postPullRequestComment_withMissingToken_throwsException() {
        GitHubRestPullRequestCommentsClient client =
                new GitHubRestPullRequestCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "");

        assertThatThrownBy(() -> client.postPullRequestComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "## AI Code Review Summary"
        ))
                .isInstanceOf(GitHubApiException.class)
                .hasMessage("GitHub API token is not configured.");
    }

    @Test
    void postPullRequestComment_withInvalidRepositoryFormat_throwsException() {
        GitHubRestPullRequestCommentsClient client =
                new GitHubRestPullRequestCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        assertThatThrownBy(() -> client.postPullRequestComment(
                "invalid-repository-name",
                42,
                "## AI Code Review Summary"
        ))
                .isInstanceOf(GitHubApiException.class)
                .hasMessage("Repository full name must use the format owner/repository.");
    }

    @Test
    void postPullRequestComment_whenGitHubReturnsError_wrapsException() {
        GitHubRestPullRequestCommentsClient client =
                new GitHubRestPullRequestCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        mockServer.expect(requestTo(
                        "https://api.github.com/repos/kellidavis/ai-code-review-assistant/issues/42/comments"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "message": "Forbidden"
                                }
                                """));

        assertThatThrownBy(() -> client.postPullRequestComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "## AI Code Review Summary"
        ))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("GitHub API returned 403")
                .hasMessageContaining("kellidavis/ai-code-review-assistant#42")
                .hasCauseInstanceOf(RestClientResponseException.class);

        mockServer.verify();
    }
}
