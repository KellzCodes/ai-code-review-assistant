package com.kellidavis.codereviewassistant.github.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class GitHubRestPullRequestCommentsClient implements GitHubPullRequestCommentsClient {
    private static final String GITHUB_JSON_MEDIA_TYPE = "application/vnd.github+json";
    private static final String GITHUB_API_VERSION_HEADER = "X-GitHub-Api-Version";
    private static final String GITHUB_API_VERSION = "2022-11-28";
    private static final int PAGE_SIZE = 100;
    private static final Pattern REPOSITORY_FULL_NAME_PATTERN = Pattern.compile("^[^/]+/[^/]+$");
    private static final ParameterizedTypeReference<List<GitHubPullRequestCommentResponse>> COMMENT_LIST_TYPE =
            new ParameterizedTypeReference<>() {};
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
    public List<GitHubPullRequestCommentResponse> listPullRequestComments(
            String repositoryFullName,
            int pullRequestNumber) {

        requireToken();
        requirePositivePullRequestNumber(pullRequestNumber);

        RepositoryCoordinates repository = parseRepositoryCoordinates(repositoryFullName);

        List<GitHubPullRequestCommentResponse> allComments = new ArrayList<>();
        int page = 1;

        while (true) {
            List<GitHubPullRequestCommentResponse> pageComments =
                    fetchCommentPage(repository, pullRequestNumber, page);

            allComments.addAll(pageComments);

            if (pageComments.size() < PAGE_SIZE) {
                break;
            }

            page++;
        }

        return List.copyOf(allComments);
    }

    @Override
    public GitHubPullRequestCommentResponse postPullRequestComment(
            String repositoryFullName,
            int pullRequestNumber,
            String commentBody) {

        requireToken();
        requirePositivePullRequestNumber(pullRequestNumber);
        requireCommentBody(commentBody);

        RepositoryCoordinates repository = parseRepositoryCoordinates(repositoryFullName);

        try {
            GitHubPullRequestCommentResponse response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/issues/{pullRequestNumber}/comments")
                            .build(repository.owner(), repository.repo(), pullRequestNumber))
                    .body(new GitHubCommentRequest(commentBody))
                    .retrieve()
                    .body(GitHubPullRequestCommentResponse.class);

            return requireResponse(
                    response,
                    "GitHub API returned an empty response while posting a pull request comment for "
                            + repository.fullName()
                            + "#"
                            + pullRequestNumber
                            + ".");
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

    @Override
    public GitHubPullRequestCommentResponse updatePullRequestComment(String repositoryFullName, long commentId, String commentBody) {

        requireToken();
        requirePositiveCommentId(commentId);
        requireCommentBody(commentBody);

        RepositoryCoordinates repository = parseRepositoryCoordinates(repositoryFullName);

        try {
            GitHubPullRequestCommentResponse response = restClient.patch()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/issues/comments/{commentId}")
                            .build(repository.owner(), repository.repo(), commentId))
                    .body(new GitHubCommentRequest(commentBody)).retrieve().body(GitHubPullRequestCommentResponse.class);

            return requireResponse(
                    response,
                    "GitHub API returned an empty response while updating pull request comment "
                            + commentId
                            + " for "
                            + repository.fullName()
                            + ".");
        } catch (RestClientResponseException ex) {
            throw new GitHubApiException(
                    "GitHub API returned "
                            + ex.getStatusCode().value()
                            + " while updating pull request comment "
                            + commentId
                            + " for "
                            + repository.fullName()
                            + ".",
                    ex);
        } catch (RestClientException ex) {
            throw new GitHubApiException(
                    "Failed to update pull request comment "
                            + commentId
                            + " for "
                            + repository.fullName()
                            + ".",
                    ex);
        }
    }

    private List<GitHubPullRequestCommentResponse> fetchCommentPage(
            RepositoryCoordinates repository,
            int pullRequestNumber,
            int page
    ) {
        try {
            List<GitHubPullRequestCommentResponse> response = restClient.get().uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/issues/{pullRequestNumber}/comments")
                            .queryParam("per_page", PAGE_SIZE).queryParam("page", page)
                            .build(repository.owner(), repository.repo(), pullRequestNumber)).retrieve()
                    .body(COMMENT_LIST_TYPE);

            return response == null ? List.of() : List.copyOf(response);
        } catch (RestClientResponseException ex) {
            throw new GitHubApiException(
                    "GitHub API returned "
                            + ex.getStatusCode().value()
                            + " while listing pull request comments for "
                            + repository.fullName()
                            + "#"
                            + pullRequestNumber
                            + ".",
                    ex);
        } catch (RestClientException ex) {
            throw new GitHubApiException(
                    "Failed to list pull request comments for "
                            + repository.fullName()
                            + "#"
                            + pullRequestNumber
                            + ".",
                    ex);
        }
    }

    private GitHubPullRequestCommentResponse requireResponse(
            GitHubPullRequestCommentResponse response,
            String emptyResponseMessage
    ) {
        if (response == null || response.id() == null || !StringUtils.hasText(response.htmlUrl())) {
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

    private void requirePositiveCommentId(long commentId) {
        if (commentId <= 0) {
            throw new GitHubApiException("Comment id must be positive.");
        }
    }

    private void requireCommentBody(String commentBody) {
        if (!StringUtils.hasText(commentBody)) {
            throw new GitHubApiException("Pull request comment body must not be blank.");
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
