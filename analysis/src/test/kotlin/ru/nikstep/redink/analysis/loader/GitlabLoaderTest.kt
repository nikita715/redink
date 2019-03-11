package ru.nikstep.redink.analysis.loader

import ru.nikstep.redink.model.entity.PullRequest
import ru.nikstep.redink.util.GitProperty
import java.time.LocalDateTime

class GitlabLoaderTest : AbstractGitLoaderTest() {
    override val repoName = "testns3/plagiarism_test"
    override val pullRequest = PullRequest(
        number = 1,
        creatorName = "testns3",
        sourceRepoId = 11158542,
        mainRepoFullName = repoName,
        sourceHeadSha = "",
        sourceBranchName = branchName,
        gitService = GitProperty.GITLAB,
        date = LocalDateTime.now(),
        sourceRepoFullName = "",
        mainRepoId = 1,
        mainHeadSha = "",
        mainBranchName = ""
    )
    override val loader = GitlabLoader(solutionStorage, repositoryRepository)
}