package com.kellidavis.codereviewassistant.github.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class GitHubRestPullRequestReviewCommentsClientTest {
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void postReviewComment_withValidRequest_returnsReviewCommentResponse() {
        GitHubRestPullRequestReviewCommentsClient client =
                new GitHubRestPullRequestReviewCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        mockServer.expect(requestTo("https://api.github.com/repos/kellidavis/ai-code-review-assistant/pulls/42/comments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(content().json("""
                        {
                          "body": "Use a logger here instead of System.out.println.",
                          "commit_id": "abc123def456",
                          "path": "src/main/java/PaymentService.java",
                          "side": "RIGHT",
                          "line": 14
                        }
                        """))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "id": 9001,
                                  "body": "Use a logger here instead of System.out.println.",
                                  "html_url": "https://github.com/kellidavis/ai-code-review-assistant/pull/42#discussion_r9001",
                                  "path": "src/main/java/PaymentService.java",
                                  "line": 14
                                }
                                """));

        GitHubPullRequestReviewCommentResponse response = client.postReviewComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "abc123def456",
                "src/main/java/PaymentService.java",
                14,
                "Use a logger here instead of System.out.println.");

        assertThat(response).isEqualTo(new GitHubPullRequestReviewCommentResponse(
                9001L,
                "Use a logger here instead of System.out.println.",
                "https://github.com/kellidavis/ai-code-review-assistant/pull/42#discussion_r9001",
                "src/main/java/PaymentService.java",
                14));

        mockServer.verify();
    }

    @Test
    void postReviewComment_withBlankRepositoryName_throwsException() {
        GitHubRestPullRequestReviewCommentsClient client =
                new GitHubRestPullRequestReviewCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        assertThatThrownBy(() -> client.postReviewComment(
                "   ",
                42,
                "abc123def456",
                "src/main/java/PaymentService.java",
                14,
                "Use a logger here instead of System.out.println."))
                .isInstanceOf(GitHubApiException.class).hasMessage("Repository full name must use the format owner/repository.");
    }

    @Test
    void postReviewComment_withInvalidRepositoryFormat_throwsException() {
        GitHubRestPullRequestReviewCommentsClient client =
                new GitHubRestPullRequestReviewCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        assertThatThrownBy(() -> client.postReviewComment(
                "invalid-repository-name",
                42,
                "abc123def456",
                "src/main/java/PaymentService.java",
                14,
                "Use a logger here instead of System.out.println."))
                .isInstanceOf(GitHubApiException.class).hasMessage("Repository full name must use the format owner/repository.");
    }

    @Test
    void postReviewComment_withNonPositivePullRequestNumber_throwsException() {
        GitHubRestPullRequestReviewCommentsClient client =
                new GitHubRestPullRequestReviewCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        assertThatThrownBy(() -> client.postReviewComment(
                "kellidavis/ai-code-review-assistant",
                0,
                "abc123def456",
                "src/main/java/PaymentService.java",
                14,
                "Use a logger here instead of System.out.println."))
                .isInstanceOf(GitHubApiException.class).hasMessage("Pull request number must be positive.");
    }

    @Test
    void postReviewComment_withBlankCommitSha_throwsException() {
        GitHubRestPullRequestReviewCommentsClient client =
                new GitHubRestPullRequestReviewCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        assertThatThrownBy(() -> client.postReviewComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "   ",
                "src/main/java/PaymentService.java",
                14,
                "Use a logger here instead of System.out.println."))
                .isInstanceOf(GitHubApiException.class).hasMessage("Commit SHA must not be blank.");
    }

    @Test
    void postReviewComment_withBlankFilePath_throwsException() {
        GitHubRestPullRequestReviewCommentsClient client =
                new GitHubRestPullRequestReviewCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        assertThatThrownBy(() -> client.postReviewComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "abc123def456",
                "   ",
                14,
                "Use a logger here instead of System.out.println."))
                .isInstanceOf(GitHubApiException.class).hasMessage("Pull request file path must not be blank.");
    }

    @Test
    void postReviewComment_withNonPositiveLineNumber_throwsException() {
        GitHubRestPullRequestReviewCommentsClient client =
                new GitHubRestPullRequestReviewCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        assertThatThrownBy(() -> client.postReviewComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "abc123def456",
                "src/main/java/PaymentService.java",
                0,
                "Use a logger here instead of System.out.println."))
                .isInstanceOf(GitHubApiException.class).hasMessage("Pull request review line number must be positive.");
    }

    @Test
    void postReviewComment_withBlankCommentBody_throwsException() {
        GitHubRestPullRequestReviewCommentsClient client =
                new GitHubRestPullRequestReviewCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        assertThatThrownBy(() -> client.postReviewComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "abc123def456",
                "src/main/java/PaymentService.java",
                14,
                "   "))
                .isInstanceOf(GitHubApiException.class).hasMessage("Pull request review comment body must not be blank.");
    }

    @Test
    void postReviewComment_withMissingToken_throwsException() {
        GitHubRestPullRequestReviewCommentsClient client =
                new GitHubRestPullRequestReviewCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "");

        assertThatThrownBy(() -> client.postReviewComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "abc123def456",
                "src/main/java/PaymentService.java",
                14,
                "Use a logger here instead of System.out.println."))
                .isInstanceOf(GitHubApiException.class).hasMessage("GitHub API token is not configured.");
    }

    @Test
    void postReviewComment_whenGitHubReturnsValidationError_wrapsException() {
        GitHubRestPullRequestReviewCommentsClient client =
                new GitHubRestPullRequestReviewCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        mockServer.expect(requestTo("https://api.github.com/repos/kellidavis/ai-code-review-assistant/pulls/42/comments"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "message": "Validation Failed"
                                }
                                """));

        assertThatThrownBy(() -> client.postReviewComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "abc123def456",
                "src/main/java/PaymentService.java",
                14,
                "Use a logger here instead of System.out.println."))
                .isInstanceOf(GitHubApiException.class).hasMessageContaining("GitHub API returned 422")
                .hasMessageContaining("while posting a pull request review comment")
                .hasCauseInstanceOf(RestClientResponseException.class);

        mockServer.verify();
    }

    @Test
    void postReviewComment_whenGitHubReturnsServerError_wrapsException() {
        GitHubRestPullRequestReviewCommentsClient client =
                new GitHubRestPullRequestReviewCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        mockServer.expect(requestTo("https://api.github.com/repos/kellidavis/ai-code-review-assistant/pulls/42/comments"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "message": "Internal Server Error"
                                }
                                """));

        assertThatThrownBy(() -> client.postReviewComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "abc123def456",
                "src/main/java/PaymentService.java",
                14,
                "Use a logger here instead of System.out.println."))
                .isInstanceOf(GitHubApiException.class).hasMessageContaining("GitHub API returned 500")
                .hasMessageContaining("while posting a pull request review comment")
                .hasCauseInstanceOf(RestClientResponseException.class);

        mockServer.verify();
    }

    @Test
    void postReviewComment_whenGitHubReturnsEmptyResponse_throwsException() {
        GitHubRestPullRequestReviewCommentsClient client =
                new GitHubRestPullRequestReviewCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        mockServer.expect(requestTo("https://api.github.com/repos/kellidavis/ai-code-review-assistant/pulls/42/comments"))
                .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON).body("{}"));

        assertThatThrownBy(() -> client.postReviewComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "abc123def456",
                "src/main/java/PaymentService.java",
                14,
                "Use a logger here instead of System.out.println."))
                .isInstanceOf(GitHubApiException.class)
                .hasMessage("GitHub API returned an empty response while posting a pull request review comment for kellidavis/ai-code-review-assistant#42.");

        mockServer.verify();
    }

    @Test
    void postReviewComment_whenRestClientFails_wrapsException() {
        GitHubRestPullRequestReviewCommentsClient client =
                new GitHubRestPullRequestReviewCommentsClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        mockServer.expect(requestTo("https://api.github.com/repos/kellidavis/ai-code-review-assistant/pulls/42/comments"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    throw new ResourceAccessException("Connection reset");
                });

        assertThatThrownBy(() -> client.postReviewComment(
                "kellidavis/ai-code-review-assistant",
                42,
                "abc123def456",
                "src/main/java/PaymentService.java",
                14,
                "Use a logger here instead of System.out.println.")).isInstanceOf(GitHubApiException.class)
                .hasMessage("Failed to post a pull request review comment for kellidavis/ai-code-review-assistant#42.")
                .hasCauseInstanceOf(ResourceAccessException.class);

        mockServer.verify();
    }
}
