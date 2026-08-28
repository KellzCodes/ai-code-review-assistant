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
public class GitHubRestPullRequestFilesClient implements GitHubPullRequestFilesClient {

    private static final String GITHUB_JSON_MEDIA_TYPE = "application/vnd.github+json";
    private static final String GITHUB_API_VERSION_HEADER = "X-GitHub-Api-Version";
    private static final String GITHUB_API_VERSION = "2022-11-28";
    private static final int PAGE_SIZE = 100;
    private static final Pattern REPOSITORY_FULL_NAME_PATTERN = Pattern.compile("^[^/]+/[^/]+$");
    private static final ParameterizedTypeReference<List<GitHubPullRequestFileResponse>> FILE_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final boolean tokenConfigured;

    public GitHubRestPullRequestFilesClient(
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
    public List<GitHubPullRequestFileResponse> fetchPullRequestFiles(
            String repositoryFullName,
            int pullRequestNumber
    ) {
        if (!tokenConfigured) {
            throw new GitHubApiException("GitHub API token is not configured.");
        }

        if (pullRequestNumber <= 0) {
            throw new GitHubApiException("Pull request number must be positive.");
        }

        RepositoryCoordinates repository = parseRepositoryCoordinates(repositoryFullName);

        List<GitHubPullRequestFileResponse> allFiles = new ArrayList<>();
        int page = 1;

        while (true) {
            List<GitHubPullRequestFileResponse> pageFiles =
                    fetchPage(repository, pullRequestNumber, page);

            allFiles.addAll(pageFiles);

            if (pageFiles.size() < PAGE_SIZE) {
                break;
            }

            page++;
        }

        return List.copyOf(allFiles);
    }

    private List<GitHubPullRequestFileResponse> fetchPage(
            RepositoryCoordinates repository,
            int pullRequestNumber,
            int page
    ) {
        try {
            List<GitHubPullRequestFileResponse> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/pulls/{pullRequestNumber}/files")
                            .queryParam("per_page", PAGE_SIZE)
                            .queryParam("page", page)
                            .build(
                                    repository.owner(),
                                    repository.repo(),
                                    pullRequestNumber
                            ))
                    .retrieve()
                    .body(FILE_LIST_TYPE);

            return response == null ? List.of() : List.copyOf(response);
        } catch (RestClientResponseException ex) {
            throw new GitHubApiException(
                    "GitHub API returned "
                            + ex.getStatusCode().value()
                            + " while fetching pull request files for "
                            + repository.fullName()
                            + "#"
                            + pullRequestNumber
                            + ".",
                    ex
            );
        } catch (RestClientException ex) {
            throw new GitHubApiException(
                    "Failed to fetch pull request files for "
                            + repository.fullName()
                            + "#"
                            + pullRequestNumber
                            + ".",
                    ex
            );
        }
    }

    private RepositoryCoordinates parseRepositoryCoordinates(String repositoryFullName) {
        if (!StringUtils.hasText(repositoryFullName)
                || !REPOSITORY_FULL_NAME_PATTERN.matcher(repositoryFullName).matches()) {
            throw new GitHubApiException(
                    "Repository full name must use the format owner/repository."
            );
        }

        String[] parts = repositoryFullName.split("/", 2);
        return new RepositoryCoordinates(parts[0], parts[1]);
    }

    private record RepositoryCoordinates(String owner, String repo) {
        String fullName() {
            return owner + "/" + repo;
        }
    }
}