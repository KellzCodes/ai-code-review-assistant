package com.kellidavis.codereviewassistant.github.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.util.regex.Pattern;

@Component
public class GitHubRestPullRequestReviewCommentsClient implements GitHubPullRequestReviewCommentsClient {
    private static final String GITHUB_JSON_MEDIA_TYPE = "application/vnd.github+json";
    private static final String GITHUB_API_VERSION_HEADER = "X-GitHub-Api-Version";
    private static final String GITHUB_API_VERSION = "2022-11-28";
    private static final Pattern REPOSITORY_FULL_NAME_PATTERN = Pattern.compile("^[^/]+/[^/]+$");
    private static final String REVIEW_COMMENT_SIDE = "RIGHT";
    private final RestClient restClient;
    private final boolean tokenConfigured;

    public GitHubRestPullRequestReviewCommentsClient(
            RestClient.Builder restClientBuilder,
            @Value("${github.api.base-url}") String baseUrl,
            @Value("${github.api.token}") String token
    ) {
        String trimmedToken = token == null ? "" : token.trim();

        this.tokenConfigured = StringUtils.hasText(trimmedToken);

        RestClient.Builder builder = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, GITHUB_JSON_MEDIA_TYPE)
                .defaultHeader(GITHUB_API_VERSION_HEADER, GITHUB_API_VERSION);

        if (this.tokenConfigured) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + trimmedToken);
        }

        this.restClient = builder.build();
    }

    @Override
    public GitHubPullRequestReviewCommentResponse postReviewComment(
            String repositoryFullName,
            int pullRequestNumber,
            String commitSha,
            String filePath,
            int lineNumber,
            String commentBody
    ) {
        requireToken();
        requirePositivePullRequestNumber(pullRequestNumber);
        requireCommitSha(commitSha);
        requireFilePath(filePath);
        requirePositiveLineNumber(lineNumber);
        requireCommentBody(commentBody);

        RepositoryCoordinates repository = parseRepositoryCoordinates(repositoryFullName);

        try {
            GitHubPullRequestReviewCommentResponse response = restClient.post().uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/pulls/{pullRequestNumber}/comments")
                            .build(repository.owner(), repository.repo(), pullRequestNumber))
                    .body(new GitHubPullRequestReviewCommentRequest(
                            commentBody,
                            commitSha.trim(),
                            filePath.trim(),
                            REVIEW_COMMENT_SIDE,
                            lineNumber)).retrieve()
                    .body(GitHubPullRequestReviewCommentResponse.class);

            return requireResponse(
                    response,
                    "GitHub API returned an empty response while posting a pull request review comment for "
                            + repository.fullName()
                            + "#"
                            + pullRequestNumber
                            + ".");
        } catch (RestClientResponseException ex) {
            throw new GitHubApiException(
                    "GitHub API returned "
                            + ex.getStatusCode().value()
                            + " while posting a pull request review comment for "
                            + repository.fullName()
                            + "#"
                            + pullRequestNumber
                            + ".",
                    ex);
        } catch (RestClientException ex) {
            throw new GitHubApiException(
                    "Failed to post a pull request review comment for "
                            + repository.fullName()
                            + "#"
                            + pullRequestNumber
                            + ".",
                    ex);
        }
    }

    private GitHubPullRequestReviewCommentResponse requireResponse(
            GitHubPullRequestReviewCommentResponse response,
            String emptyResponseMessage
    ) {
        if (response == null
                || response.id() == null
                || !StringUtils.hasText(response.htmlUrl())) {
            throw new GitHubApiException(emptyResponseMessage);
        }

        return response;
    }

    private void requireToken() {
        if (!tokenConfigured) {
            throw new GitHubApiException("GitHub API token is not configured.");
        }
    }

    private void requirePositivePullRequestNumber(int pullRequestNumber) {
        if (pullRequestNumber <= 0) {
            throw new GitHubApiException("Pull request number must be positive.");
        }
    }

    private void requireCommitSha(String commitSha) {
        if (!StringUtils.hasText(commitSha)) {
            throw new GitHubApiException("Commit SHA must not be blank.");
        }
    }

    private void requireFilePath(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            throw new GitHubApiException("Pull request file path must not be blank.");
        }
    }

    private void requirePositiveLineNumber(int lineNumber) {
        if (lineNumber <= 0) {
            throw new GitHubApiException("Pull request review line number must be positive.");
        }
    }

    private void requireCommentBody(String commentBody) {
        if (!StringUtils.hasText(commentBody)) {
            throw new GitHubApiException("Pull request review comment body must not be blank.");
        }
    }

    private RepositoryCoordinates parseRepositoryCoordinates(String repositoryFullName) {
        if (!StringUtils.hasText(repositoryFullName)
                || !REPOSITORY_FULL_NAME_PATTERN.matcher(repositoryFullName).matches()) {
            throw new GitHubApiException("Repository full name must use the format owner/repository.");
        }

        String[] parts = repositoryFullName.split("/", 2);
        return new RepositoryCoordinates(parts[0], parts[1]);
    }

    private record RepositoryCoordinates(String owner, String repo) {
        String fullName() {
            return owner + "/" + repo;
        }
    }

    private record GitHubPullRequestReviewCommentRequest(
            String body,
            @JsonProperty("commit_id")
            String commitId,
            String path,
            String side,
            int line
    ) {
    }
}
