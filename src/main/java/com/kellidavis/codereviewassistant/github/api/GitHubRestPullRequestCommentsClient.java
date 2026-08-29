package com.kellidavis.codereviewassistant.github.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.util.regex.Pattern;

@Component
public class GitHubRestPullRequestCommentsClient implements GitHubPullRequestCommentsClient {
    private static final String GITHUB_JSON_MEDIA_TYPE = "application/vnd.github+json";
    private static final String GITHUB_API_VERSION_HEADER = "X-GitHub-Api-Version";
    private static final String GITHUB_API_VERSION = "2022-11-28";
    private static final Pattern REPOSITORY_FULL_NAME_PATTERN = Pattern.compile("^[^/]+/[^/]+$");
    private final RestClient restClient;
    private final boolean tokenConfigured;

    public GitHubRestPullRequestCommentsClient(
            RestClient.Builder restClientBuilder,
            @Value("${github.api.base-url}") String baseUrl,
            @Value("${github.api.token}") String token) {

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
    public GitHubPullRequestCommentResponse postPullRequestComment(
            String repositoryFullName,
            int pullRequestNumber,
            String commentBody) {

        if (!tokenConfigured) {
            throw new GitHubApiException("GitHub API token is not configured.");
        }

        if (pullRequestNumber <= 0) {
            throw new GitHubApiException("Pull request number must be positive.");
        }

        if (!StringUtils.hasText(commentBody)) {
            throw new GitHubApiException("Pull request comment body must not be blank.");
        }

        RepositoryCoordinates repository = parseRepositoryCoordinates(repositoryFullName);

        try {
            GitHubPullRequestCommentResponse response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/issues/{pullRequestNumber}/comments")
                            .build(repository.owner(), repository.repo(), pullRequestNumber))
                    .body(new GitHubCommentRequest(commentBody))
                    .retrieve()
                    .body(GitHubPullRequestCommentResponse.class);

            if (response == null || !StringUtils.hasText(response.htmlUrl())) {
                throw new GitHubApiException(
                        "GitHub API returned an empty response while posting a pull request comment for "
                                + repository.fullName()
                                + "#"
                                + pullRequestNumber
                                + ".");
            }

            return response;
        } catch (RestClientResponseException ex) {
            throw new GitHubApiException(
                    "GitHub API returned "
                            + ex.getStatusCode().value()
                            + " while posting a pull request comment for "
                            + repository.fullName()
                            + "#"
                            + pullRequestNumber
                            + ".",
                    ex);
        } catch (RestClientException ex) {
            throw new GitHubApiException(
                    "Failed to post a pull request comment for "
                            + repository.fullName()
                            + "#"
                            + pullRequestNumber
                            + ".",
                    ex);
        }
    }

    private RepositoryCoordinates parseRepositoryCoordinates(String repositoryFullName) {
        if (!StringUtils.hasText(repositoryFullName)
                || !REPOSITORY_FULL_NAME_PATTERN.matcher(repositoryFullName).matches()) {
            throw new GitHubApiException(
                    "Repository full name must use the format owner/repository.");
        }

        String[] parts = repositoryFullName.split("/", 2);
        return new RepositoryCoordinates(parts[0], parts[1]);
    }

    private record RepositoryCoordinates(String owner, String repo) {
        String fullName() {
            return owner + "/" + repo;
        }
    }

    private record GitHubCommentRequest(String body) {
    }
}
