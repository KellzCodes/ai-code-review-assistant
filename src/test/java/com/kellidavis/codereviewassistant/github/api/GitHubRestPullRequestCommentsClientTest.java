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
    void listPullRequestComments_withValidResponse_returnsComments() {
        GitHubRestPullRequestCommentsClient client =
                new GitHubRestPullRequestCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        mockServer.expect(requestTo("https://api.github.com/repos/kellidavis/ai-code-review-assistant/issues/42/comments?per_page=100&page=1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": 1001,
                            "body": "first comment",
                            "html_url": "https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1001"
                          },
                          {
                            "id": 1002,
                            "body": "second comment",
                            "html_url": "https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1002"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.listPullRequestComments("kellidavis/ai-code-review-assistant", 42))
                .containsExactly(
                        new GitHubPullRequestCommentResponse(
                                1001L,
                                "first comment",
                                "https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1001"
                        ),
                        new GitHubPullRequestCommentResponse(
                                1002L,
                                "second comment",
                                "https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1002"
                        )
                );

        mockServer.verify();
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
                          "id": 1001,
                          "body": "## AI Code Review Summary",
                          "html_url": "https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1"
                        }
                        """, MediaType.APPLICATION_JSON));

        GitHubPullRequestCommentResponse response = client.postPullRequestComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "## AI Code Review Summary");

        assertThat(response.id()).isEqualTo(1001L);
        assertThat(response.body()).isEqualTo("## AI Code Review Summary");
        assertThat(response.htmlUrl())
                .isEqualTo("https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1");

        mockServer.verify();
    }

    @Test
    void updatePullRequestComment_withValidRequest_returnsUpdatedCommentResponse() {
        GitHubRestPullRequestCommentsClient client =
                new GitHubRestPullRequestCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        mockServer.expect(requestTo("https://api.github.com/repos/kellidavis/ai-code-review-assistant/issues/comments/1001"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(content().json("""
                        {
                          "body": "Updated summary comment"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "id": 1001,
                          "body": "Updated summary comment",
                          "html_url": "https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1001"
                        }
                        """, MediaType.APPLICATION_JSON));

        GitHubPullRequestCommentResponse response = client.updatePullRequestComment(
                "kellidavis/ai-code-review-assistant",
                1001L,
                "Updated summary comment");

        assertThat(response).isEqualTo(new GitHubPullRequestCommentResponse(
                1001L,
                "Updated summary comment",
                "https://github.com/kellidavis/ai-code-review-assistant/pull/42#issuecomment-1001"));

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
    void updatePullRequestComment_withInvalidCommentId_throwsException() {
        GitHubRestPullRequestCommentsClient client =
                new GitHubRestPullRequestCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        assertThatThrownBy(() -> client.updatePullRequestComment(
                "kellidavis/ai-code-review-assistant",
                0L,
                "Updated summary comment"
        ))
                .isInstanceOf(GitHubApiException.class)
                .hasMessage("Comment id must be positive.");
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
                "## AI Code Review Summary")).isInstanceOf(GitHubApiException.class)
                .hasMessage("GitHub API token is not configured.");
    }

    @Test
    void listPullRequestComments_whenGitHubReturnsError_wrapsException() {
        GitHubRestPullRequestCommentsClient client =
                new GitHubRestPullRequestCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        mockServer.expect(requestTo(
                        "https://api.github.com/repos/kellidavis/ai-code-review-assistant/issues/42/comments?per_page=100&page=1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "message": "Forbidden"
                                }
                                """));

        assertThatThrownBy(() -> client.listPullRequestComments(
                "kellidavis/ai-code-review-assistant",
                42
        ))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("GitHub API returned 403")
                .hasMessageContaining("while listing pull request comments")
                .hasCauseInstanceOf(RestClientResponseException.class);

        mockServer.verify();
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
    void updatePullRequestComment_whenGitHubReturnsError_wrapsException() {
        GitHubRestPullRequestCommentsClient client =
                new GitHubRestPullRequestCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        mockServer.expect(requestTo(
                        "https://api.github.com/repos/kellidavis/ai-code-review-assistant/issues/comments/1001"))
                .andExpect(method(HttpMethod.PATCH)).andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "message": "Forbidden"
                                }
                                """));

        assertThatThrownBy(() -> client.updatePullRequestComment(
                "kellidavis/ai-code-review-assistant",
                1001L,
                "Updated summary comment")).isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("GitHub API returned 403")
                .hasMessageContaining("while updating pull request comment 1001")
                .hasCauseInstanceOf(RestClientResponseException.class);

        mockServer.verify();
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
