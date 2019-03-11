package ru.nikstep.redink.model.manager

import org.springframework.transaction.annotation.Transactional
import ru.nikstep.redink.model.entity.Repository
import ru.nikstep.redink.model.repo.RepositoryRepository
import ru.nikstep.redink.model.repo.UserRepository
import ru.nikstep.redink.util.AnalyserProperty
import ru.nikstep.redink.util.AnalysisMode
import ru.nikstep.redink.util.GitProperty
import ru.nikstep.redink.util.Language

/**
 * Data manager of [Repository]
 */
open class RepositoryDataManager(
    private val repositoryRepository: RepositoryRepository,
    private val userRepository: UserRepository
) {

    /**
     * Create repositories by [repoNames]
     */
    @Transactional
    open fun create(ownerName: String, gitProperty: GitProperty, repoNames: List<String>) {
        val owner = userRepository.findByName(ownerName)
        repoNames.forEach { repoName ->
            repositoryRepository.save(
                Repository(
                    language = Language.JAVA,
                    owner = owner,
                    name = repoName,
                    analysisMode = AnalysisMode.STATIC,
                    gitService = gitProperty,
                    analyser = AnalyserProperty.MOSS,
                    branches = listOf("master")
                )
            )
        }
    }

    /**
     * Delete repositories by [repoNames]
     */
    @Transactional
    open fun delete(repoNames: List<String>) {
        repoNames.forEach { repoName ->
            repositoryRepository.deleteByGitServiceAndName(
                GitProperty.GITHUB,
                repoName
            )
        }
    }

    /**
     * Save all [repositories]
     */
    @Transactional
    open fun saveAll(repositories: List<Repository>) {
        repositoryRepository.saveAll(repositories)
    }

}