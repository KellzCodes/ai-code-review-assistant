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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubRestPullRequestFilesClientTest {
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void fetchPullRequestFiles_withValidResponse_returnsFiles() {
        GitHubRestPullRequestFilesClient client =
                new GitHubRestPullRequestFilesClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token"
                );

        String responseBody = """
                [
                  {
                    "filename": "src/main/java/PaymentService.java",
                    "status": "modified",
                    "patch": "@@ -1,3 +1,4 @@",
                    "additions": 2,
                    "deletions": 1,
                    "changes": 3
                  },
                  {
                    "filename": "src/main/java/OrderService.java",
                    "status": "renamed",
                    "patch": "@@ -1,2 +1,2 @@",
                    "additions": 1,
                    "deletions": 1,
                    "changes": 2,
                    "previous_filename": "src/main/java/LegacyOrderService.java"
                  }
                ]
                """;

        mockServer.expect(requestTo(
                        "https://api.github.com/repos/kellidavis/ai-code-review-assistant/pulls/42/files?per_page=100&page=1"))
                .andExpect(method(HttpMethod.GET)).andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        List<GitHubPullRequestFileResponse> files = client.fetchPullRequestFiles(
                "kellidavis/ai-code-review-assistant",
                42);

        assertThat(files).hasSize(2);

        GitHubPullRequestFileResponse firstFile = files.get(0);
        assertThat(firstFile.filename()).isEqualTo("src/main/java/PaymentService.java");
        assertThat(firstFile.status()).isEqualTo("modified");
        assertThat(firstFile.patch()).isEqualTo("@@ -1,3 +1,4 @@");
        assertThat(firstFile.additions()).isEqualTo(2);
        assertThat(firstFile.deletions()).isEqualTo(1);
        assertThat(firstFile.changes()).isEqualTo(3);
        assertThat(firstFile.previousFilename()).isNull();

        GitHubPullRequestFileResponse secondFile = files.get(1);
        assertThat(secondFile.filename()).isEqualTo("src/main/java/OrderService.java");
        assertThat(secondFile.status()).isEqualTo("renamed");
        assertThat(secondFile.previousFilename()).isEqualTo("src/main/java/LegacyOrderService.java");

        mockServer.verify();
    }

    @Test
    void fetchPullRequestFiles_withMultiplePages_returnsCombinedFiles() {
        GitHubRestPullRequestFilesClient client =
                new GitHubRestPullRequestFilesClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        mockServer.expect(requestTo(
                        "https://api.github.com/repos/kellidavis/ai-code-review-assistant/pulls/42/files?per_page=100&page=1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(buildFilesJson(1, 100), MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(
                        "https://api.github.com/repos/kellidavis/ai-code-review-assistant/pulls/42/files?per_page=100&page=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(buildFilesJson(101, 1), MediaType.APPLICATION_JSON));

        List<GitHubPullRequestFileResponse> files = client.fetchPullRequestFiles(
                "kellidavis/ai-code-review-assistant",
                42);

        assertThat(files).hasSize(101);
        assertThat(files.get(0).filename()).isEqualTo("src/main/java/File1.java");
        assertThat(files.get(99).filename()).isEqualTo("src/main/java/File100.java");
        assertThat(files.get(100).filename()).isEqualTo("src/main/java/File101.java");

        mockServer.verify();
    }

    @Test
    void fetchPullRequestFiles_withInvalidRepositoryFormat_throwsException() {
        GitHubRestPullRequestFilesClient client =
                new GitHubRestPullRequestFilesClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        assertThatThrownBy(() -> client.fetchPullRequestFiles("invalid-repository-name", 42))
                .isInstanceOf(GitHubApiException.class).hasMessage("Repository full name must use the format owner/repository.");
    }

    @Test
    void fetchPullRequestFiles_withMissingToken_throwsException() {
        GitHubRestPullRequestFilesClient client =
                new GitHubRestPullRequestFilesClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "   ");

        assertThatThrownBy(() -> client.fetchPullRequestFiles(
                "kellidavis/ai-code-review-assistant", 42))
                .isInstanceOf(GitHubApiException.class).hasMessage("GitHub API token is not configured.");
    }

    @Test
    void fetchPullRequestFiles_whenGitHubReturnsError_wrapsException() {
        GitHubRestPullRequestFilesClient client =
                new GitHubRestPullRequestFilesClient(
                        restClientBuilder,
                        "https://api.github.com",
                        "test-token");

        mockServer.expect(requestTo(
                        "https://api.github.com/repos/kellidavis/ai-code-review-assistant/pulls/42/files?per_page=100&page=1"))
                .andExpect(method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "message": "Forbidden"
                                }
                                """));

        assertThatThrownBy(() -> client.fetchPullRequestFiles(
                "kellidavis/ai-code-review-assistant",
                42
        ))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("GitHub API returned 403")
                .hasMessageContaining("kellidavis/ai-code-review-assistant#42")
                .hasCauseInstanceOf(RestClientResponseException.class);

        mockServer.verify();
    }

    private String buildFilesJson(int startIndex, int count) {
        return IntStream.range(startIndex, startIndex + count)
                .mapToObj(index -> """
                        {
                          "filename": "src/main/java/File%d.java",
                          "status": "modified",
                          "patch": "@@ -1,1 +1,1 @@",
                          "additions": 1,
                          "deletions": 1,
                          "changes": 2
                        }
                        """.formatted(index))
                .collect(Collectors.joining(",\n", "[\n", "\n]"));
    }
}