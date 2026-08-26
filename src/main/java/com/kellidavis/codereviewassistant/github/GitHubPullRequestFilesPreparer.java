package com.kellidavis.codereviewassistant.github;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class GitHubPullRequestFilesPreparer {
    private static final String REMOVED_STATUS = "removed";

    private final PullRequestFileLanguageResolver languageResolver;

    public GitHubPullRequestFilesPreparer(PullRequestFileLanguageResolver languageResolver) {
        this.languageResolver = languageResolver;
    }

    public PullRequestFilePreparationResult prepareFiles(List<GitHubPullRequestFileResponse> changedFiles) {
        if (changedFiles == null) {
            return new PullRequestFilePreparationResult(List.of(), 0, 0);
        }

        List<PreparedPullRequestFile> preparedFiles = new ArrayList<>();
        int skippedFiles = 0;

        for (GitHubPullRequestFileResponse changedFile : changedFiles) {
            if (shouldSkip(changedFile)) {
                skippedFiles++;
                continue;
            }

            preparedFiles.add(new PreparedPullRequestFile(
                    changedFile.filename(),
                    languageResolver.resolveLanguage(changedFile.filename()),
                    changedFile.status(),
                    changedFile.patch(),
                    changedFile.additions(),
                    changedFile.deletions()));
        }

        return new PullRequestFilePreparationResult(
                preparedFiles,
                changedFiles.size(),
                skippedFiles);
    }

    private boolean shouldSkip(GitHubPullRequestFileResponse changedFile) {
        return changedFile == null
                || hasRemovedStatus(changedFile.status())
                || changedFile.patch() == null
                || changedFile.patch().isBlank();
    }

    private boolean hasRemovedStatus(String status) {
        return status != null && REMOVED_STATUS.equalsIgnoreCase(status);
    }
}
